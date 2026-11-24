package vn.co.cake.controller.external.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import vn.co.cake.entity.Order;
import vn.co.cake.entity.OrderItem;
import vn.co.cake.entity.PancakeProperties;
import vn.co.cake.entity.Variation;
import vn.co.cake.entity.Warehouse;

class MainOrderRequestCharacterizationTest {

    private static final String FULL_NAME = "Nguyen Van A";
    private static final String PHONE = "0901234567";
    private static final String ORDER_CODE = "DB0001";
    private static final String ADDRESS = "1 Le Loi, Q1, HCM";
    private static final String NOTE = "Giao gio hanh chinh";

    private Order order;
    private PancakeProperties properties;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setFullName(FULL_NAME);
        order.setPhone(PHONE);
        order.setCode(ORDER_CODE);
        order.setShippingAddress(ADDRESS);
        order.setNote(NOTE);
        order.setShippingFee(new BigDecimal("30000"));

        properties = new PancakeProperties();
        properties.setPageId("page-1");
        properties.setSellerId("seller-1");
        properties.setWarehouseId("wh-1");
        properties.setShopId("12345");

        warehouse = new Warehouse();
        warehouse.setDistrictId("d-1");
        warehouse.setFullAddress("Kho 1");
        warehouse.setName("Main");
        warehouse.setPhoneNumber("0280000000");
        warehouse.setProvinceId("p-1");
    }

    private static OrderItem orderItem(int quantity, BigDecimal price, BigDecimal discountPrice, Long retailPrice) {
        Variation variation = new Variation();
        variation.setVariationId(String.format("var-%d", quantity));
        variation.setPancakeProductId(String.format("prod-%d", quantity));
        variation.setDisplayId(String.format("disp-%d", quantity));
        variation.setName(String.format("Name %d", quantity));
        variation.setRetailPrice(retailPrice);
        OrderItem item = new OrderItem();
        item.setVariation(variation);
        item.setQuantity(quantity);
        item.setPrice(price);
        item.setDiscountPrice(discountPrice);
        return item;
    }

    @Test
    void mapsHeaderFieldsAndConstants() {
        MainOrderRequest request = new MainOrderRequest(order, properties, warehouse, Collections.emptyList());

        assertEquals(FULL_NAME, request.getBill_full_name());
        assertEquals(FULL_NAME, request.getAccount_name());
        assertEquals(PHONE, request.getBill_phone_number());
        assertTrue(request.is_free_shipping());
        assertFalse(request.isReceived_at_shop());
        assertEquals(1, request.getAccount());
        assertEquals(0, request.getReturned_reason());
        assertEquals("page-1", request.getPage_id());
        assertEquals("seller-1", request.getAssigning_seller_id());
        assertEquals("wh-1", request.getWarehouse_id());
        assertEquals(12345, request.getShop_id());
        assertEquals(30000, request.getShipping_fee());
        assertEquals(ORDER_CODE, request.getCustom_id());
        assertEquals(NOTE, request.getNote_print());
        assertEquals(0, request.getDiscount());
        assertEquals(0, request.getPrepaid());
        assertTrue(request.getItems().isEmpty());
        assertNull(request.getActivatedPromotionAdvances());
    }

    @Test
    void noteEmbedsShippingFeeUsingBigDecimalToString() {
        order.setShippingFee(new BigDecimal("30000.50"));
        MainOrderRequest request = new MainOrderRequest(order, properties, warehouse, Collections.emptyList());

        // current behavior: note shows the raw BigDecimal (with scale) while shipping_fee is truncated
        assertEquals("WEB + 30000.50 tiền phí ship setting trên web Debase", request.getNote());
        assertEquals(30000, request.getShipping_fee());
    }

    @Test
    void prepaidIsSetOnlyWhenPositive() {
        order.setPrepaid(new BigDecimal("50000.9"));
        assertEquals(50000, new MainOrderRequest(order, properties, warehouse, Collections.emptyList()).getPrepaid());

        order.setPrepaid(new BigDecimal("-1"));
        assertEquals(0, new MainOrderRequest(order, properties, warehouse, Collections.emptyList()).getPrepaid());

        order.setPrepaid(BigDecimal.ZERO);
        assertEquals(0, new MainOrderRequest(order, properties, warehouse, Collections.emptyList()).getPrepaid());
    }

    @Test
    void discountIsSumOfUnitDiscountTimesQuantity() {
        List<OrderItem> items = Arrays.asList(
                orderItem(2, new BigDecimal("100000"), new BigDecimal("10000"), 120000L),
                orderItem(3, new BigDecimal("50000"), new BigDecimal("-5"), 50000L),
                orderItem(1, new BigDecimal("50000"), null, 50000L));

        MainOrderRequest request = new MainOrderRequest(order, properties, warehouse, items);

        assertEquals(20000, request.getDiscount());
    }

    @Test
    void mapsItemsAndVariationInfo() {
        List<OrderItem> items = Collections.singletonList(
                orderItem(2, new BigDecimal("99000.9"), null, 120000L));

        MainOrderRequest request = new MainOrderRequest(order, properties, warehouse, items);

        Item item = request.getItems().get(0);
        assertEquals(2, item.getQuantity());
        assertEquals("var-2", item.getVariation_id());
        assertEquals("prod-2", item.getProduct_id());
        assertEquals(0, item.getDiscount_each_product());
        assertFalse(item.is_bonus_product());
        assertFalse(item.is_discount_percent());
        assertFalse(item.is_wholesale());
        assertFalse(item.isOne_time_product());
        VariationInfo info = item.getVariation_info();
        assertEquals("disp-2", info.getDisplay_id());
        // current behavior: product_display_id is the variation displayId, not a product id
        assertEquals("disp-2", info.getProduct_display_id());
        assertEquals("Name 2", info.getName());
        assertEquals(99000, info.getRetail_price());
        assertEquals(200, info.getWeight());
        assertNull(info.getDetail());
        assertNull(info.getFields());
    }

    @Test
    void retailPriceFallsBackToVariationThenZero() {
        List<OrderItem> items = Arrays.asList(
                orderItem(1, BigDecimal.ZERO, null, 120000L),
                orderItem(2, null, null, 80000L),
                orderItem(3, null, null, null));

        MainOrderRequest request = new MainOrderRequest(order, properties, warehouse, items);

        assertEquals(120000, request.getItems().get(0).getVariation_info().getRetail_price());
        assertEquals(80000, request.getItems().get(1).getVariation_info().getRetail_price());
        assertEquals(0, request.getItems().get(2).getVariation_info().getRetail_price());
    }

    @Test
    void mapsShippingAddressAndWarehouseInfo() {
        MainOrderRequest request = new MainOrderRequest(order, properties, warehouse, Collections.emptyList());

        ShippingAddress address = request.getShipping_address();
        assertEquals(FULL_NAME, address.getFull_name());
        assertEquals(PHONE, address.getPhone_number());
        assertEquals(ADDRESS, address.getAddress());
        assertEquals(ADDRESS, address.getFull_address());
        assertNull(address.getProvince_id());
        assertNull(address.getDistrict_id());
        assertNull(address.getCommune_id());

        WarehouseInfo info = request.getWarehouse_info();
        assertEquals("d-1", info.getDistrictId());
        assertEquals("Kho 1", info.getFullAddress());
        assertEquals("Main", info.getName());
        assertEquals("0280000000", info.getPhoneNumber());
        assertEquals("p-1", info.getProvinceId());
    }

    @Test
    void serializesWithSnakeCasePropertyNames() {
        List<OrderItem> items = Collections.singletonList(
                orderItem(2, new BigDecimal("99000"), new BigDecimal("1000"), 120000L));
        MainOrderRequest request = new MainOrderRequest(order, properties, warehouse, items);

        JsonNode json = new ObjectMapper().valueToTree(request);

        assertEquals(FULL_NAME, json.get("bill_full_name").asText());
        assertEquals(PHONE, json.get("bill_phone_number").asText());
        // current behavior: Lombok getter is_free_shipping() makes Jackson emit "_free_shipping", not "is_free_shipping"
        assertFalse(json.has("is_free_shipping"));
        assertTrue(json.get("_free_shipping").asBoolean());
        assertFalse(json.get("received_at_shop").asBoolean());
        assertEquals(1, json.get("account").asInt());
        assertEquals(12345, json.get("shop_id").asInt());
        assertEquals(30000, json.get("shipping_fee").asInt());
        assertEquals(2000, json.get("discount").asInt());
        assertEquals(0, json.get("prepaid").asInt());
        assertEquals(ORDER_CODE, json.get("custom_id").asText());
        assertTrue(json.get("activatedPromotionAdvances").isNull());

        JsonNode item = json.get("items").get(0);
        assertEquals("var-2", item.get("variation_id").asText());
        assertEquals("prod-2", item.get("product_id").asText());
        // current behavior: same "is" prefix stripping on item flags
        assertFalse(item.has("is_bonus_product"));
        assertFalse(item.get("_bonus_product").asBoolean());
        assertFalse(item.get("_discount_percent").asBoolean());
        assertFalse(item.get("_wholesale").asBoolean());
        assertFalse(item.get("one_time_product").asBoolean());
        assertEquals(99000, item.get("variation_info").get("retail_price").asInt());
        assertEquals(200, item.get("variation_info").get("weight").asInt());

        assertEquals(ADDRESS, json.get("shipping_address").get("full_address").asText());
        // current behavior: warehouse_info nested fields are serialized camelCase, unlike the rest of the payload
        JsonNode warehouseInfo = json.get("warehouse_info");
        assertEquals("d-1", warehouseInfo.get("districtId").asText());
        assertEquals("Kho 1", warehouseInfo.get("fullAddress").asText());
        assertEquals("0280000000", warehouseInfo.get("phoneNumber").asText());
        assertEquals("p-1", warehouseInfo.get("provinceId").asText());
    }

    @Test
    void nullShippingFeeThrowsNullPointerException() {
        order.setShippingFee(null);
        assertThrows(NullPointerException.class,
                () -> new MainOrderRequest(order, properties, warehouse, Collections.emptyList()));
    }

    @Test
    void nonNumericShopIdThrowsNumberFormatException() {
        properties.setShopId("shop-abc");
        assertThrows(NumberFormatException.class,
                () -> new MainOrderRequest(order, properties, warehouse, Collections.emptyList()));
    }

    @Test
    void itemWithNullVariationThrowsNullPointerException() {
        OrderItem item = orderItem(1, BigDecimal.TEN, null, 10L);
        item.setVariation(null);
        List<OrderItem> items = Collections.singletonList(item);
        assertThrows(NullPointerException.class,
                () -> new MainOrderRequest(order, properties, warehouse, items));
    }

    @Test
    void nullWarehouseOrNullItemListThrowsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new MainOrderRequest(order, properties, null, Collections.emptyList()));
        assertThrows(NullPointerException.class,
                () -> new MainOrderRequest(order, properties, warehouse, null));
    }
}
