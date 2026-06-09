package vn.co.cake.controller.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderPancakeResponse {
    private String id;
    @JsonProperty("custom_id")
    private String customId;
    private String order_link;
    private String bill_full_name;
    private String money_to_collect;
    private String link_confirm_order;
    private String tracking_link;
    private String inserted_at;
    private String status_name;
    @JsonProperty("partner_name")
    private String partnerName;
    private ShippingAddress shipping_address;

    public static List<OrderPancakeResponse> parseListFromData(JsonNode dataNode, ObjectMapper mapper) {
        List<OrderPancakeResponse> orders = new ArrayList<>();
        if (dataNode == null || dataNode.isNull() || !dataNode.isArray()) {
            return orders;
        }
        Iterator<JsonNode> elements = dataNode.elements();
        while (elements.hasNext()) {
            try {
                orders.add(fromPancakeJsonNode(elements.next(), mapper));
            } catch (Exception ignored) {
                // Skip malformed order payload; continue with others
            }
        }
        return orders;
    }

    public static OrderPancakeResponse fromPancakeJsonNode(JsonNode node, ObjectMapper mapper) {
        OrderPancakeResponse order = new OrderPancakeResponse();
        if (node == null || node.isNull()) {
            return order;
        }
        order.setId(scalarText(node, "id"));
        order.setCustomId(scalarText(node, "custom_id"));
        order.setOrder_link(firstLink(node, "order_link", "order_url", "webcms_order_link", "shop_order_link"));
        order.setBill_full_name(scalarText(node, "bill_full_name"));
        order.setMoney_to_collect(scalarText(node, "money_to_collect", "cod"));
        order.setLink_confirm_order(scalarText(node, "link_confirm_order"));
        order.setTracking_link(firstLink(node, "tracking_link"));
        order.setInserted_at(scalarText(node, "inserted_at"));
        order.setStatus_name(resolveStatusName(node));
        order.setPartnerName(resolvePartnerName(node));
        order.setShipping_address(parseShippingAddress(node, mapper));
        return order;
    }

    private static String resolvePartnerName(JsonNode node) {
        String partnerName = scalarText(node, "partner_name");
        if (StringUtils.isNotBlank(partnerName)) {
            return partnerName.trim();
        }
        JsonNode partnerNode = node.get("partner");
        if (partnerNode == null || partnerNode.isNull()) {
            return null;
        }
        if (partnerNode.isTextual()) {
            return partnerNode.asText().trim();
        }
        if (partnerNode.isObject()) {
            String fromObject = scalarText(partnerNode, "name", "short_name", "code");
            if (StringUtils.isNotBlank(fromObject)) {
                return fromObject.trim();
            }
        }
        return null;
    }

    private static String resolveStatusName(JsonNode node) {
        String statusName = scalarText(node, "status_name");
        if (StringUtils.isNotBlank(statusName)) {
            return statusName;
        }
        JsonNode status = node.get("status");
        if (status != null && !status.isNull()) {
            if (status.isTextual()) {
                return status.asText();
            }
            if (status.isNumber()) {
                return "Trạng thái #" + status.asInt();
            }
        }
        return null;
    }

    private static ShippingAddress parseShippingAddress(JsonNode node, ObjectMapper mapper) {
        JsonNode shippingNode = node.get("shipping_address");
        ShippingAddress address = null;
        if (shippingNode != null && shippingNode.isObject()) {
            try {
                address = mapper.convertValue(shippingNode, ShippingAddress.class);
            } catch (Exception ignored) {
                address = null;
            }
        }
        if (address == null) {
            address = new ShippingAddress();
        }
        if (StringUtils.isBlank(address.getFull_name())) {
            address.setFull_name(scalarText(node, "bill_full_name"));
        }
        if (StringUtils.isBlank(address.getPhone_number())) {
            address.setPhone_number(scalarText(node, "bill_phone_number", "phone_number"));
        }
        if (StringUtils.isBlank(address.getFull_address())) {
            address.setFull_address(scalarText(node, "bill_address", "address", "full_address"));
        }
        return address;
    }

    private static String firstLink(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = scalarText(node, field);
            if (StringUtils.isNotBlank(value) && (value.startsWith("http://") || value.startsWith("https://"))) {
                return value;
            }
        }
        return null;
    }

    private static String scalarText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isTextual() || value.isNumber() || value.isBoolean()) {
                return value.asText();
            }
        }
        return null;
    }

    /** Gộp field hiển thị từ API order detail (ưu tiên tracking_link từ detail). */
    public void mergeFromDetail(OrderPancakeResponse detail) {
        if (detail == null) {
            return;
        }
        if (StringUtils.isNotBlank(detail.getTracking_link())) {
            this.tracking_link = detail.getTracking_link();
        }
        if (StringUtils.isNotBlank(detail.getStatus_name())) {
            this.status_name = detail.getStatus_name();
        }
        if (StringUtils.isNotBlank(detail.getOrder_link())) {
            this.order_link = detail.getOrder_link();
        }
        if (StringUtils.isNotBlank(detail.getBill_full_name())) {
            this.bill_full_name = detail.getBill_full_name();
        }
        if (StringUtils.isNotBlank(detail.getMoney_to_collect())) {
            this.money_to_collect = detail.getMoney_to_collect();
        }
        if (StringUtils.isNotBlank(detail.getInserted_at())) {
            this.inserted_at = detail.getInserted_at();
        }
        if (detail.getShipping_address() != null) {
            this.shipping_address = detail.getShipping_address();
        }
        if (StringUtils.isNotBlank(detail.getPartnerName())) {
            this.partnerName = detail.getPartnerName();
        }
    }

    public String resolveShippingPartner() {
        if (StringUtils.isNotBlank(partnerName)) {
            return partnerName.trim();
        }
        return null;
    }
}
