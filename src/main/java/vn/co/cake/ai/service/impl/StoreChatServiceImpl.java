package vn.co.cake.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.co.cake.ai.dto.storechat.StoreChatProductCard;
import vn.co.cake.ai.dto.storechat.StoreChatRequest;
import vn.co.cake.ai.dto.storechat.StoreChatResponse;
import vn.co.cake.ai.dto.storechat.StoreChatVariationDto;
import vn.co.cake.ai.service.StoreChatService;
import vn.co.cake.ai.service.openai.OpenAIService;
import vn.co.cake.common.BaseConst;
import vn.co.cake.common.StringConst;
import vn.co.cake.entity.Account;
import vn.co.cake.entity.Product;
import vn.co.cake.entity.Variation;
import vn.co.cake.repository.AccountRepository;
import vn.co.cake.repository.ProductRepository;
import vn.co.cake.repository.VariationRepository;
import vn.co.cake.request.ProductSearchRequest;
import vn.co.cake.response.ProductResponse;
import vn.co.cake.security.user.UserLoginInfo;

import javax.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tư vấn chỉ dựa data website:
 * SALE = discount &gt; 0; NEW IN / BEST SELLER / danh mục = categories.
 * Loại món cụ thể (áo phông, sơ mi…) → lọc name exact trước; không có mới gợi ý tương tự.
 */
@Slf4j
@Service
public class StoreChatServiceImpl implements StoreChatService {

    private static final int QUERY_LIMIT = 24;
    private static final int RESULT_LIMIT = 6;
    private static final List<String> SIZE_ORDER = Arrays.asList("S", "M", "L", "XL", "XXL", "XXXL", "2XL", "3XL");
    private static final Pattern SIZE_PATTERN = Pattern.compile("\\b(xxxl|xxl|xl|[sml])\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEIGHT_METER = Pattern.compile("(\\d)\\s*[mM]\\s*(\\d{1,2})");
    private static final Pattern HEIGHT_CM = Pattern.compile("(?:cao|chiều cao)\\s*(?:là|:)?\\s*(\\d{2,3})\\s*(?:cm)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEIGHT_CM_PLAIN = Pattern.compile("(\\d{3})\\s*cm", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEIGHT_KG = Pattern.compile("(?:nặng|cân nặng)?\\s*(\\d{2,3})\\s*kg", Pattern.CASE_INSENSITIVE);

    /** Ưu tiên loại cụ thể trước từ khóa chung như "áo". */
    private static final List<ProductTypeDef> PRODUCT_TYPES = Arrays.asList(
            new ProductTypeDef("áo phông", "TOP",
                    Arrays.asList("áo phông", "ao phong", "phông", "phong", "tee", "t-shirt", "tshirt", "logo tee"),
                    Arrays.asList("sơ mi", "somi", "oxford", "shirt")),
            new ProductTypeDef("sơ mi", "TOP",
                    Arrays.asList("sơ mi", "somi", "oxford"),
                    Arrays.asList("phông", "phong", "tee", "t-shirt", "tshirt", "hoodie", "nỉ", "sweat")),
            new ProductTypeDef("hoodie", "TOP",
                    Arrays.asList("hoodie"),
                    Arrays.asList("sơ mi", "somi", "oxford", "phông", "tee")),
            new ProductTypeDef("áo nỉ", "TOP",
                    Arrays.asList("áo nỉ", "ao ni", "nỉ", "sweater", "sweatshirt", "sweat"),
                    Arrays.asList("sơ mi", "somi", "oxford", "phông", "tee", "t-shirt")),
            new ProductTypeDef("quần jean", "BOTTOM",
                    Arrays.asList("quần jean", "quan jean", "jean", "jeans"),
                    Arrays.asList("kaki", "nỉ", "short")),
            new ProductTypeDef("quần kaki", "BOTTOM",
                    Arrays.asList("quần kaki", "quan kaki", "kaki"),
                    Arrays.asList("jean", "jeans")),
            new ProductTypeDef("oversize", null,
                    Arrays.asList("oversize"),
                    Arrays.asList())
    );

    private final ProductRepository productRepository;
    private final VariationRepository variationRepository;
    private final AccountRepository accountRepository;
    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper;

    public StoreChatServiceImpl(ProductRepository productRepository,
                                VariationRepository variationRepository,
                                AccountRepository accountRepository,
                                OpenAIService openAIService) {
        this.productRepository = productRepository;
        this.variationRepository = variationRepository;
        this.accountRepository = accountRepository;
        this.openAIService = openAIService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public StoreChatResponse chat(StoreChatRequest request, HttpSession session) {
        String userMessage = request != null ? StringUtils.trimToEmpty(request.getMessage()) : "";
        ChatIntent intent = classify(userMessage);
        BodyProfile body = resolveBodyProfile(request, session, userMessage);
        QueryResult queryResult = resolveProducts(intent);

        log.info("Store chat intent={}, productType={}, mode={}, dbHits={}, height={}, weight={}",
                intent.type, intent.productTypeLabel, queryResult.mode, queryResult.products.size(),
                body.height, body.weight);

        if (queryResult.products.isEmpty()) {
            return StoreChatResponse.builder()
                    .message(emptyReply(intent))
                    .products(new ArrayList<>())
                    .needBodyInfo(false)
                    .similarSuggestion(false)
                    .height(body.height)
                    .weight(body.weight)
                    .build();
        }

        List<Product> shown = queryResult.products.stream().limit(RESULT_LIMIT).collect(Collectors.toList());
        boolean needBodyInfo = !body.complete();
        boolean similar = queryResult.mode == QueryMode.SIMILAR;
        String factualReply = factualReply(intent, queryResult, shown);
        if (needBodyInfo) {
            factualReply += askBodyQuestion(body);
        }

        Map<Long, String> recommendedSizes = new LinkedHashMap<>();
        List<Product> ranked = shown;
        String reply = factualReply;
        try {
            String aiJson = openAIService.callGPT(
                    buildSystemPrompt(intent, body, queryResult.mode),
                    buildUserPrompt(userMessage, shown, request, intent, body, queryResult.mode));
            AiPick pick = parseAiPick(aiJson);
            ranked = rankWithinCatalog(pick, shown);
            recommendedSizes = resolveRecommendedSizes(ranked, pick, body);
            reply = StringUtils.isNotBlank(pick.reply) ? pick.reply : factualReply;
            if (needBodyInfo && !reply.contains("chiều cao") && !reply.contains("cân nặng")) {
                reply += askBodyQuestion(body);
            }
            if (similar && !reply.toLowerCase(Locale.ROOT).contains("tương tự")
                    && !reply.toLowerCase(Locale.ROOT).contains("không có")) {
                reply = similarPrefix(intent) + " " + reply;
            }
        } catch (Exception e) {
            log.warn("Store chat AI skipped, returning DB result: {}", e.getMessage());
            recommendedSizes = resolveRecommendedSizes(shown, new AiPick(), body);
        }

        return StoreChatResponse.builder()
                .message(reply)
                .products(toCards(ranked, recommendedSizes))
                .needBodyInfo(needBodyInfo)
                .similarSuggestion(similar)
                .height(body.height)
                .weight(body.weight)
                .build();
    }

    private ChatIntent classify(String message) {
        ChatIntent intent = new ChatIntent();
        if (StringUtils.isBlank(message)) {
            intent.type = IntentType.NEW_IN;
            intent.navCategory = StringConst.NEW_IN;
            return intent;
        }
        String lower = message.toLowerCase(Locale.ROOT);

        ProductTypeDef productType = detectProductType(lower);
        if (productType != null) {
            intent.productTypeLabel = productType.label;
            intent.matchTokens = productType.matchTokens;
            intent.excludeTokens = productType.excludeTokens;
            intent.nameKeyword = productType.primarySearchToken();
            intent.shopHint = productType.shopHint;
        } else {
            intent.shopHint = inferShopCategory(lower);
        }

        if (containsAny(lower, "sale", "giảm giá", "giam gia", "khuyến mãi", "khuyen mai", "đang sale")) {
            intent.type = IntentType.SALE;
            intent.navCategory = "SALE";
        } else if (containsAny(lower, "hàng mới", "hang moi", "new in", "mới về", "moi ve", "newin")
                && productType == null) {
            intent.type = IntentType.NEW_IN;
            intent.navCategory = StringConst.NEW_IN;
        } else if (containsAny(lower, "best seller", "bán chạy", "ban chay", "bestseller")
                && productType == null) {
            intent.type = IntentType.NAV;
            intent.navCategory = "BEST SELLER";
        } else if (productType != null) {
            // Loại món cụ thể luôn SEARCH theo tên — không đổ cả TOP
            intent.type = IntentType.SEARCH;
        } else if (StringUtils.isNotBlank(intent.shopHint)) {
            intent.type = IntentType.NAV;
            intent.navCategory = intent.shopHint;
        } else {
            intent.type = IntentType.SEARCH;
            intent.nameKeyword = extractGenericKeyword(lower);
        }

        if (StringUtils.isBlank(intent.nameKeyword) && productType == null) {
            intent.nameKeyword = extractNameKeywordLegacy(lower, intent);
        }
        intent.size = extractSize(lower);
        intent.color = extractColor(lower);
        return intent;
    }

    private ProductTypeDef detectProductType(String lower) {
        for (ProductTypeDef def : PRODUCT_TYPES) {
            for (String token : def.matchTokens) {
                if (lower.contains(token)) {
                    // "shirt" trong t-shirt không phải sơ mi
                    if ("shirt".equals(token) && (lower.contains("t-shirt") || lower.contains("tshirt"))) {
                        continue;
                    }
                    return def;
                }
            }
        }
        // sơ mi: thêm "shirt" khi không phải t-shirt
        if (lower.contains("shirt") && !lower.contains("t-shirt") && !lower.contains("tshirt")) {
            return PRODUCT_TYPES.stream().filter(d -> "sơ mi".equals(d.label)).findFirst().orElse(null);
        }
        return null;
    }

    private String extractNameKeywordLegacy(String lower, ChatIntent intent) {
        if (lower.contains("oversize")) return "oversize";
        if (lower.contains("hoodie")) return "hoodie";
        if (lower.contains("sơ mi") || lower.contains("somi") || lower.contains("oxford")) return "sơ mi";
        if (lower.contains("jean")) return "jean";
        if (lower.contains("kaki")) return "kaki";
        if (lower.contains("sweater") || lower.contains("nỉ") || lower.contains("sweat")) return "sweat";
        if (intent.type == IntentType.SEARCH) {
            return extractGenericKeyword(lower);
        }
        return null;
    }

    private String extractGenericKeyword(String lower) {
        String cleaned = lower.replaceAll("[^\\p{L}\\p{N}\\s-]", " ").trim();
        cleaned = cleaned.replaceAll("\\b(tôi|mình|muốn|tìm|cho|mặc|size|của|bạn|gợi ý|goi y|xem|mua|tư vấn|tu van)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return StringUtils.isBlank(cleaned) ? null : StringUtils.abbreviate(cleaned, 40);
    }

    private String extractSize(String lower) {
        Matcher matcher = SIZE_PATTERN.matcher(lower);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase(Locale.ROOT);
        }
        return null;
    }

    private String extractColor(String lower) {
        if (lower.contains("đen") || lower.contains("black")) return "BLACK";
        if (lower.contains("trắng") || lower.contains("white")) return "WHITE";
        if (lower.contains("be") || lower.contains("beige")) return null;
        if (lower.contains("xám") || lower.contains("gray") || lower.contains("grey")) return "GRAY";
        if (lower.contains("xanh dương") || lower.contains("navy") || lower.contains("blue")) return "BLUE";
        if (lower.contains("xanh lá") || lower.contains("green")) return "GREEN";
        if (lower.contains("đỏ") || lower.contains("red")) return "RED";
        if (lower.contains("hồng") || lower.contains("pink")) return "PINK";
        if (lower.contains("vàng") || lower.contains("yellow")) return "YELLOW";
        if (lower.contains("cam") || lower.contains("orange")) return "ORANGE";
        if (lower.contains("tím") || lower.contains("purple")) return "PURPLE";
        return null;
    }

    private QueryResult resolveProducts(ChatIntent intent) {
        if (hasProductTypeFilter(intent)) {
            List<Product> exact = queryExactByProductType(intent);
            if (!exact.isEmpty()) {
                return new QueryResult(QueryMode.EXACT, exact);
            }
            List<Product> similar = querySimilar(intent);
            return new QueryResult(QueryMode.SIMILAR, similar);
        }
        List<Product> products = queryByCategoryOrSearch(intent);
        return new QueryResult(QueryMode.EXACT, products);
    }

    private boolean hasProductTypeFilter(ChatIntent intent) {
        return intent.matchTokens != null && !intent.matchTokens.isEmpty();
    }

    private List<Product> queryExactByProductType(ChatIntent intent) {
        Map<Long, Product> unique = new LinkedHashMap<>();
        List<String> searchTokens = new ArrayList<>();
        for (String token : intent.matchTokens) {
            if (StringUtils.isNotBlank(token) && token.length() >= 3 && !searchTokens.contains(token)) {
                searchTokens.add(token);
            }
        }
        if (searchTokens.isEmpty() && StringUtils.isNotBlank(intent.nameKeyword)) {
            searchTokens.add(intent.nameKeyword);
        }
        for (String token : searchTokens) {
            ProductSearchRequest search = new ProductSearchRequest();
            search.setName(token);
            if (intent.type == IntentType.SALE) {
                search.setCategory("SALE");
            }
            applySizeColor(search, intent);
            for (Product p : fetch(search)) {
                unique.putIfAbsent(p.getId(), p);
            }
        }
        List<Product> products = new ArrayList<>(unique.values());
        if (intent.type == IntentType.SALE) {
            products = products.stream().filter(this::isOnSale).collect(Collectors.toList());
        }
        return products.stream()
                .filter(p -> matchesProductType(p, intent))
                .filter(p -> !excludedByTokens(p, intent.excludeTokens))
                .collect(Collectors.toList());
    }

    private List<Product> querySimilar(ChatIntent intent) {
        ProductSearchRequest search = new ProductSearchRequest();
        if (StringUtils.isNotBlank(intent.shopHint)) {
            search.setCategory(intent.shopHint);
        } else if (intent.type == IntentType.SALE) {
            search.setCategory("SALE");
        }
        applySizeColor(search, intent);
        List<Product> products = fetch(search);
        if (intent.type == IntentType.SALE) {
            products = products.stream().filter(this::isOnSale).collect(Collectors.toList());
        }
        if (StringUtils.isNotBlank(intent.shopHint)) {
            products = products.stream().filter(p -> hasCategory(p, intent.shopHint)).collect(Collectors.toList());
        }
        // Gợi ý tương tự: cùng danh mục nhưng loại trừ loại lệch (vd hỏi phông thì bỏ sơ mi)
        return products.stream()
                .filter(p -> !matchesProductType(p, intent))
                .filter(p -> !excludedByTokens(p, intent.excludeTokens))
                .collect(Collectors.toList());
    }

    private List<Product> queryByCategoryOrSearch(ChatIntent intent) {
        ProductSearchRequest search = new ProductSearchRequest();
        if (StringUtils.isNotBlank(intent.navCategory)) {
            search.setCategory(intent.navCategory);
        }
        if (StringUtils.isNotBlank(intent.nameKeyword)
                && (intent.type == IntentType.SEARCH || intent.type == IntentType.SALE)) {
            search.setName(intent.nameKeyword);
        }
        applySizeColor(search, intent);
        List<Product> products = fetch(search);

        if (intent.type == IntentType.SALE) {
            products = products.stream().filter(this::isOnSale).collect(Collectors.toList());
            if (StringUtils.isNotBlank(intent.shopHint)) {
                List<Product> inShop = products.stream()
                        .filter(p -> hasCategory(p, intent.shopHint))
                        .collect(Collectors.toList());
                if (!inShop.isEmpty()) {
                    products = inShop;
                }
            }
        }
        if (intent.type == IntentType.NEW_IN) {
            products = products.stream().filter(p -> hasCategory(p, StringConst.NEW_IN)).collect(Collectors.toList());
        }
        if (intent.type == IntentType.NAV && StringUtils.isNotBlank(intent.navCategory)) {
            products = products.stream().filter(p -> hasCategory(p, intent.navCategory)).collect(Collectors.toList());
        }
        return products;
    }

    private void applySizeColor(ProductSearchRequest search, ChatIntent intent) {
        if (StringUtils.isNotBlank(intent.size)) {
            search.setSizeProduct(intent.size);
        }
        if (StringUtils.isNotBlank(intent.color)) {
            search.setColor(intent.color);
        }
    }

    private boolean matchesProductType(Product product, ChatIntent intent) {
        if (intent.matchTokens == null || intent.matchTokens.isEmpty()) {
            return true;
        }
        String name = product.getName() == null ? "" : product.getName().toLowerCase(Locale.ROOT);
        for (String token : intent.matchTokens) {
            if ("shirt".equals(token) && (name.contains("t-shirt") || name.contains("tshirt"))) {
                continue;
            }
            if (name.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean excludedByTokens(Product product, List<String> excludeTokens) {
        if (excludeTokens == null || excludeTokens.isEmpty()) {
            return false;
        }
        String name = product.getName() == null ? "" : product.getName().toLowerCase(Locale.ROOT);
        for (String token : excludeTokens) {
            if ("shirt".equals(token) && (name.contains("t-shirt") || name.contains("tshirt"))) {
                continue;
            }
            if (name.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private List<Product> fetch(ProductSearchRequest search) {
        Page<Product> page = productRepository.getAllByCondition(search, PageRequest.of(0, QUERY_LIMIT), false);
        return page.getContent();
    }

    private boolean isOnSale(Product product) {
        return product.getDiscount() != null && product.getDiscount().compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean hasCategory(Product product, String category) {
        return product.getCategories() != null
                && product.getCategories().toLowerCase(Locale.ROOT).contains(category.toLowerCase(Locale.ROOT));
    }

    /** Chỉ xếp hạng trong catalog đã lọc — không pad thêm SP ngoài list AI chọn. */
    private List<Product> rankWithinCatalog(AiPick pick, List<Product> catalog) {
        Map<Long, Product> byId = catalog.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a, LinkedHashMap::new));
        List<Product> ranked = new ArrayList<>();
        if (pick.productIds != null) {
            for (Long id : pick.productIds) {
                Product p = byId.get(id);
                if (p != null && ranked.stream().noneMatch(x -> x.getId() == p.getId())) {
                    ranked.add(p);
                }
                if (ranked.size() >= RESULT_LIMIT) {
                    break;
                }
            }
        }
        if (ranked.isEmpty()) {
            return catalog.stream().limit(RESULT_LIMIT).collect(Collectors.toList());
        }
        return ranked;
    }

    private String factualReply(ChatIntent intent, QueryResult queryResult, List<Product> shown) {
        if (queryResult.mode == QueryMode.SIMILAR) {
            return similarPrefix(intent)
                    + " Mình gợi ý " + shown.size()
                    + " món gần đúng trong cùng nhóm. Bấm Xem hoặc Thêm giỏ nếu bạn muốn.";
        }
        if (StringUtils.isNotBlank(intent.productTypeLabel)) {
            return "Có " + queryResult.products.size() + " món \"" + intent.productTypeLabel
                    + "\" khớp yêu cầu. Gửi bạn " + shown.size()
                    + " món. Bấm Xem để vào chi tiết hoặc Thêm giỏ để mua.";
        }
        String source;
        switch (intent.type) {
            case SALE:
                source = "các sản phẩm đang SALE trên website";
                break;
            case NEW_IN:
                source = "nhóm NEW IN trên website";
                break;
            case NAV:
                source = "danh mục " + intent.navCategory + " trên website";
                break;
            default:
                source = "kết quả tìm theo tên trên website";
        }
        return "Mình lấy đúng " + source + ". Có " + queryResult.products.size() + " món khớp, gửi bạn "
                + shown.size() + " món. Bấm Xem để vào chi tiết hoặc Thêm giỏ để mua.";
    }

    private String similarPrefix(ChatIntent intent) {
        String label = StringUtils.defaultIfBlank(intent.productTypeLabel, "sản phẩm bạn hỏi");
        return "Hiện không có \"" + label + "\" khớp đúng yêu cầu.";
    }

    private String emptyReply(ChatIntent intent) {
        if (StringUtils.isNotBlank(intent.productTypeLabel)) {
            return "Hiện không có \"" + intent.productTypeLabel
                    + "\" và cũng chưa có món tương tự phù hợp. Bạn thử từ khóa khác giúp mình nhé.";
        }
        switch (intent.type) {
            case SALE:
                return "Hiện không có sản phẩm đang giảm giá. Bạn xem NEW IN hoặc tìm theo tên giúp mình nhé.";
            case NEW_IN:
                return "Hiện chưa có sản phẩm trong nhóm NEW IN. Bạn thử BEST SELLER hoặc tìm theo tên nhé.";
            case NAV:
                return "Hiện chưa có sản phẩm trong danh mục " + intent.navCategory + ".";
            default:
                return "Không tìm thấy sản phẩm khớp trên website. Bạn thử tên gần đúng hơn (ví dụ: jean, sơ mi, áo phông).";
        }
    }

    private String buildSystemPrompt(ChatIntent intent, BodyProfile body, QueryMode mode) {
        String sizeRule = body.complete()
                ? "Khách có chiều cao " + body.height + "cm, cân nặng " + body.weight + "kg.\n" +
                  "BẮT BUỘC chọn size từ BẢNG ĐO (description_size) của từng sản phẩm, không đoán bừa.\n" +
                  "Thứ tự rộng: S→M→L→XL→XXL. Trả sizes {\"productId\":\"M\"} với size CÓ TRONG sizes của sản phẩm.\n"
                : "Khách CHƯA có chiều cao/cân nặng. ĐỪNG chọn size mặc định (đặc biệt không gắn XL).\n" +
                  "Hãy hỏi thêm chiều cao (cm) và cân nặng (kg) để tư vấn size từ bảng đo sản phẩm.\n";
        String modeRule = mode == QueryMode.SIMILAR
                ? "mode=SIMILAR: catalog là gợi ý thay thế. Reply PHẢI nói rõ không có đúng loại khách hỏi rồi mới gợi ý tương tự.\n"
                : "mode=EXACT: catalog đã khớp đúng loại khách hỏi. Chỉ recommend các id này, không đề xuất loại khác.\n";
        String typeRule = StringUtils.isNotBlank(intent.productTypeLabel)
                ? "Khách hỏi: \"" + intent.productTypeLabel + "\". CẤM recommend sản phẩm lệch loại.\n"
                : "";
        return "Bạn là stylist De Basé. Catalog ĐÃ LỌC từ database, intent=" + intent.type + ".\n" +
                modeRule + typeRule +
                "Nguồn SP: SALE=discount>0; NEW IN/BEST SELLER/TOP/BOTTOM=categories; search=name.\n" +
                sizeRule +
                "CHỈ dùng productId trong catalog. CẤM bịa sản phẩm.\n" +
                "JSON thuần: {\"reply\":\"...\",\"productIds\":[1,2],\"sizes\":{\"1\":\"M\",\"2\":\"L\"}}\n";
    }

    private String buildUserPrompt(String message, List<Product> candidates, StoreChatRequest request,
                                   ChatIntent intent, BodyProfile body, QueryMode mode) {
        StringBuilder sb = new StringBuilder();
        sb.append("mode=").append(mode)
                .append(" | Intent: ").append(intent.type)
                .append(" | productType=").append(intent.productTypeLabel)
                .append(" | nav=").append(intent.navCategory)
                .append(" | name=").append(intent.nameKeyword).append("\n");
        if (intent.matchTokens != null && !intent.matchTokens.isEmpty()) {
            sb.append("matchTokens=").append(intent.matchTokens)
                    .append(" | excludeTokens=").append(intent.excludeTokens).append("\n");
        }
        if (body.complete()) {
            sb.append("Khách: ").append(body.height).append("cm / ").append(body.weight).append("kg")
                    .append(" (nguồn: ").append(body.source).append(")\n");
        } else {
            sb.append("Khách chưa có đủ chiều cao/cân nặng.\n");
        }
        sb.append("Câu hỏi: ").append(message).append("\n\n");
        if (request != null && request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            int start = Math.max(0, request.getConversationHistory().size() - 4);
            for (int i = start; i < request.getConversationHistory().size(); i++) {
                Map<String, String> turn = request.getConversationHistory().get(i);
                sb.append(turn.getOrDefault("role", "user")).append(": ")
                        .append(turn.getOrDefault("content", "")).append("\n");
            }
            sb.append("\n");
        }
        sb.append("CATALOG:\n");
        for (Product p : candidates) {
            ProductResponse view = new ProductResponse(p);
            sb.append("- id=").append(p.getId())
                    .append(" | ").append(p.getName())
                    .append(" | giá=").append(view.getDiscountPriceDisplay())
                    .append(" | sizes=").append(StringUtils.defaultString(p.getSizes()))
                    .append(" | discount%=").append(p.getDiscount())
                    .append(" | categories=").append(StringUtils.defaultString(p.getCategories()))
                    .append("\n  BẢNG ĐO (description_size): ")
                    .append(trimSizeChart(p.getDescriptionSize()))
                    .append("\n");
        }
        return sb.toString();
    }

    private AiPick parseAiPick(String aiJson) {
        AiPick pick = new AiPick();
        try {
            String json = extractJson(aiJson);
            JsonNode root = objectMapper.readTree(json);
            pick.reply = root.path("reply").asText("");
            JsonNode ids = root.path("productIds");
            if (ids.isArray()) {
                pick.productIds = new ArrayList<>();
                for (JsonNode idNode : ids) {
                    if (idNode.isNumber()) {
                        pick.productIds.add(idNode.asLong());
                    } else if (idNode.isTextual() && StringUtils.isNumeric(idNode.asText())) {
                        pick.productIds.add(Long.parseLong(idNode.asText()));
                    }
                }
            }
            pick.sizesByProductId = new LinkedHashMap<>();
            JsonNode sizes = root.path("sizes");
            if (sizes.isObject()) {
                sizes.fields().forEachRemaining(entry -> {
                    if (StringUtils.isNumeric(entry.getKey())) {
                        pick.sizesByProductId.put(Long.parseLong(entry.getKey()), entry.getValue().asText());
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Cannot parse store-chat AI JSON: {}", e.getMessage());
        }
        return pick;
    }

    private String extractJson(String response) {
        if (StringUtils.isBlank(response)) {
            return "{}";
        }
        String trimmed = response.trim();
        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private List<StoreChatProductCard> toCards(List<Product> products, Map<Long, String> recommendedSizes) {
        List<StoreChatProductCard> cards = new ArrayList<>();
        if (products == null) {
            return cards;
        }
        for (Product product : products) {
            String rec = recommendedSizes != null ? recommendedSizes.get(product.getId()) : null;
            cards.add(toCard(product, rec));
        }
        return cards;
    }

    private StoreChatProductCard toCard(Product product, String recommendedSize) {
        ProductResponse view = new ProductResponse(product);
        List<StoreChatVariationDto> variations = new ArrayList<>();
        if (StringUtils.isNotBlank(product.getProductPancakeId())) {
            List<Variation> vars = variationRepository.findAllByPancakeProductId(product.getProductPancakeId());
            if (vars != null) {
                for (Variation v : vars) {
                    if (v.isDeleted()) {
                        continue;
                    }
                    variations.add(StoreChatVariationDto.builder()
                            .variationId(v.getVariationId())
                            .color(v.getColor())
                            .size(v.getSize())
                            .type(v.getType())
                            .image(v.getImage())
                            .remainQuantity(v.getRemainQuantity())
                            .build());
                }
            }
        }
        List<String> types = new ArrayList<>();
        if (StringUtils.isNotBlank(product.getTypes())) {
            types = Arrays.stream(product.getTypes().split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return StoreChatProductCard.builder()
                .id(product.getId())
                .name(product.getName())
                .image(product.getImage())
                .priceDisplay(view.getPriceDisplay())
                .discountPriceDisplay(view.getDiscountPriceDisplay())
                .finalPrice(view.getFinalPrice())
                .hasDiscount(view.isHasDiscount())
                .outStock(view.isOutStock())
                .detailUrl("/products?id=" + product.getId())
                .colors(view.getColors())
                .sizes(sortSizes(view.getSizes()))
                .types(types)
                .variations(variations)
                .recommendedSize(recommendedSize)
                .build();
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private enum IntentType {
        SALE, NEW_IN, NAV, SEARCH
    }

    private enum QueryMode {
        EXACT, SIMILAR
    }

    private static class QueryResult {
        private final QueryMode mode;
        private final List<Product> products;

        QueryResult(QueryMode mode, List<Product> products) {
            this.mode = mode;
            this.products = products != null ? products : new ArrayList<>();
        }
    }

    private static class ProductTypeDef {
        private final String label;
        private final String shopHint;
        private final List<String> matchTokens;
        private final List<String> excludeTokens;

        ProductTypeDef(String label, String shopHint, List<String> matchTokens, List<String> excludeTokens) {
            this.label = label;
            this.shopHint = shopHint;
            this.matchTokens = matchTokens;
            this.excludeTokens = excludeTokens;
        }

        String primarySearchToken() {
            // Token ngắn để DB contains: "phông", "tee", "sơ mi"...
            for (String t : matchTokens) {
                if (t.length() >= 3 && !t.contains(" ")) {
                    return t;
                }
            }
            return matchTokens.isEmpty() ? label : matchTokens.get(0);
        }
    }

    private static class ChatIntent {
        private IntentType type = IntentType.SEARCH;
        private String navCategory;
        private String nameKeyword;
        private String size;
        private String color;
        private String shopHint;
        private String productTypeLabel;
        private List<String> matchTokens;
        private List<String> excludeTokens;
    }

    private String inferShopCategory(String lower) {
        if (containsAny(lower, "outwear", "khoác", "jacket", "áo khoác")) return "OUTWEAR";
        if (containsAny(lower, "bottom", "quần", "jean", "pants")) return "BOTTOM";
        if (containsAny(lower, "giày", "dép", "túi", "bag", "shoes", "sandal")) return "SHOES_BAG";
        if (containsAny(lower, "phụ kiện", "accessory")) return "ACC";
        // Chỉ map TOP khi KH nói danh mục chung, không nói loại món cụ thể
        if (containsAny(lower, " top", "danh mục top", "xem top") || lower.trim().equals("top") || lower.trim().equals("áo")) {
            return "TOP";
        }
        return null;
    }

    private static class AiPick {
        private String reply;
        private List<Long> productIds;
        private Map<Long, String> sizesByProductId;
    }

    private static class BodyProfile {
        private Integer height;
        private Integer weight;
        private String gender = "male";
        private String source = "none";

        boolean complete() {
            return height != null && height > 0 && weight != null && weight > 0;
        }
    }

    private BodyProfile resolveBodyProfile(StoreChatRequest request, HttpSession session, String message) {
        BodyProfile body = new BodyProfile();
        parseBodyFromMessage(message, body);
        if (request != null) {
            if (!has(body.height) && request.getHeight() != null && request.getHeight() > 0) {
                body.height = request.getHeight();
                body.source = "request";
            }
            if (!has(body.weight) && request.getWeight() != null && request.getWeight() > 0) {
                body.weight = request.getWeight();
                if ("none".equals(body.source)) body.source = "request";
            }
            if (StringUtils.isNotBlank(request.getGender())) {
                body.gender = request.getGender();
            }
        }
        if ((!has(body.height) || !has(body.weight)) && session != null) {
            UserLoginInfo loginInfo = (UserLoginInfo) session.getAttribute(BaseConst.USER_SESSION);
            if (loginInfo != null && loginInfo.getId() != null) {
                Account account = accountRepository.findFirstByIdAndDeletedIsFalse(loginInfo.getId());
                if (account != null) {
                    if (!has(body.height) && account.getHeight() != null && account.getHeight() > 0) {
                        body.height = account.getHeight();
                        body.source = "account";
                    }
                    if (!has(body.weight) && account.getWeight() != null && account.getWeight() > 0) {
                        body.weight = account.getWeight();
                        if (!"message".equals(body.source)) body.source = "account";
                    }
                }
            }
        }
        if (containsAny(message.toLowerCase(Locale.ROOT), "nữ", "chi", "chị")) {
            body.gender = "female";
        } else if (containsAny(message.toLowerCase(Locale.ROOT), "nam", "anh")) {
            body.gender = "male";
        }
        return body;
    }

    private void parseBodyFromMessage(String message, BodyProfile body) {
        if (StringUtils.isBlank(message)) {
            return;
        }
        Matcher meter = HEIGHT_METER.matcher(message);
        if (meter.find()) {
            int m = Integer.parseInt(meter.group(1));
            int cmPart = Integer.parseInt(meter.group(2));
            if (cmPart < 10) {
                cmPart = cmPart * 10;
            }
            int height = m * 100 + cmPart;
            if (height >= 120 && height <= 220) {
                body.height = height;
                body.source = "message";
            }
        }
        if (!has(body.height)) {
            Matcher h1 = HEIGHT_CM.matcher(message);
            Matcher h2 = HEIGHT_CM_PLAIN.matcher(message);
            String h = h1.find() ? h1.group(1) : (h2.find() ? h2.group(1) : null);
            if (h != null) {
                int height = Integer.parseInt(h);
                if (height >= 120 && height <= 220) {
                    body.height = height;
                    body.source = "message";
                }
            }
        }
        Matcher w = WEIGHT_KG.matcher(message);
        if (w.find()) {
            int weight = Integer.parseInt(w.group(1));
            if (weight >= 35 && weight <= 150) {
                body.weight = weight;
                if (!"message".equals(body.source)) body.source = "message";
            }
        }
    }

    private boolean has(Integer v) {
        return v != null && v > 0;
    }

    private String askBodyQuestion(BodyProfile body) {
        if (!has(body.height) && !has(body.weight)) {
            return " Bạn cho mình thêm chiều cao (cm) và cân nặng (kg) để tư vấn size theo bảng đo sản phẩm nhé. Ví dụ: cao 165cm nặng 55kg.";
        }
        if (!has(body.height)) {
            return " Mình đã có cân nặng " + body.weight + "kg. Bạn cho thêm chiều cao (cm) để chốt size giúp mình.";
        }
        return " Mình đã có chiều cao " + body.height + "cm. Bạn cho thêm cân nặng (kg) để chốt size giúp mình.";
    }

    private Map<Long, String> resolveRecommendedSizes(List<Product> products, AiPick pick, BodyProfile body) {
        Map<Long, String> result = new LinkedHashMap<>();
        if (products == null || !body.complete()) {
            return result;
        }
        for (Product product : products) {
            String fromAi = pick.sizesByProductId != null ? pick.sizesByProductId.get(product.getId()) : null;
            String size = normalizeAvailableSize(fromAi, product);
            if (StringUtils.isBlank(size)) {
                size = normalizeAvailableSize(chartSize(body), product);
            }
            if (StringUtils.isNotBlank(size)) {
                result.put(product.getId(), size);
            }
        }
        return result;
    }

    private String chartSize(BodyProfile body) {
        int weight = body.weight;
        if ("female".equalsIgnoreCase(body.gender)) {
            if (weight <= 47) return "S";
            if (weight <= 55) return "M";
            if (weight <= 62) return "L";
            return "XL";
        }
        if (weight <= 55) return "S";
        if (weight <= 65) return "M";
        if (weight <= 75) return "L";
        if (weight <= 85) return "XL";
        return "XXL";
    }

    private String normalizeAvailableSize(String raw, Product product) {
        if (StringUtils.isBlank(raw) || product == null || StringUtils.isBlank(product.getSizes())) {
            return null;
        }
        String want = raw.trim().toUpperCase(Locale.ROOT).replace("SIZE ", "");
        if ("2XL".equals(want)) want = "XXL";
        List<String> available = Arrays.stream(product.getSizes().split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        if (available.contains(want)) {
            return want;
        }
        int idx = SIZE_ORDER.indexOf(want);
        if (idx < 0) {
            return null;
        }
        for (int delta = 0; delta < SIZE_ORDER.size(); delta++) {
            int down = idx - delta;
            int up = idx + delta;
            if (down >= 0 && available.contains(SIZE_ORDER.get(down))) return SIZE_ORDER.get(down);
            if (up < SIZE_ORDER.size() && available.contains(SIZE_ORDER.get(up))) return SIZE_ORDER.get(up);
        }
        return null;
    }

    private List<String> sortSizes(List<String> sizes) {
        if (sizes == null) {
            return new ArrayList<>();
        }
        List<String> copy = new ArrayList<>(sizes);
        copy.sort((a, b) -> {
            int ia = SIZE_ORDER.indexOf(a.trim().toUpperCase(Locale.ROOT));
            int ib = SIZE_ORDER.indexOf(b.trim().toUpperCase(Locale.ROOT));
            if (ia < 0) ia = 99;
            if (ib < 0) ib = 99;
            return Integer.compare(ia, ib);
        });
        return copy;
    }

    private String trimSizeChart(String html) {
        if (StringUtils.isBlank(html)) {
            return "(không có bảng đo)";
        }
        String cleaned = html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() > 700) {
            return cleaned.substring(0, 700);
        }
        return cleaned;
    }
}
