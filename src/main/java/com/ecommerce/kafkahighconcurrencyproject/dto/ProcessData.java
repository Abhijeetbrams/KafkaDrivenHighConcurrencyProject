package com.ecommerce.kafkahighconcurrencyproject.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ProcessData {

    @JsonProperty("tracking_id")
    private String trackingId = "";

    @JsonProperty("merchant_id")
    private String merchantId = "";

    @JsonProperty("movement_type")
    private String movementType = "";

    @JsonProperty("client_id")
    private String clientId = "";

    @JsonProperty("pickup_date")
    private String pickupDate = "";

    @JsonProperty("mh_inscan_date")
    private String mhInscanDate = "";

    @JsonProperty("shipment_type")
    private String shipmentType = "";

    @JsonProperty("shipment_status")
    private String shipmentStatus = "";

    @JsonProperty("shipment_status_date")
    private String shipmentStatusDate = "";

    @JsonProperty("zone")
    private String zone = "";

    @JsonProperty("source_city")
    private String sourceCity = "";

    @JsonProperty("destination_city")
    private String destinationCity = "";

    @JsonProperty("source_zone")
    private String sourceZone = "";

    @JsonProperty("destination_zone")
    private String destinationZone = "";

    @JsonProperty("source_zstate")
    private String sourceZstate = "";

    @JsonProperty("destination_zstate")
    private String destinationZstate = "";

    @JsonProperty("source_tier")
    private String sourceTier = "";

    @JsonProperty("destination_tier")
    private String destinationTier = "";

    @JsonProperty("payment_type")
    private String paymentType = "";

    @JsonProperty("seller_dead_weight")
    private String sellerDeadWeight = "";

    @JsonProperty("profiler_dead_weight")
    private String profilerDeadWeight = "";

    @JsonProperty("source_pincode")
    private String sourcePincode = "";

    @JsonProperty("destination_pincode")
    private String destinationPincode = "";

    @JsonProperty("shipment_value")
    private String shipmentValue = "";

    @JsonProperty("amount_to_collect")
    private String amountToCollect = "";

    @JsonProperty("seller_declared_length")
    private String sellerDeclaredLength = "";

    @JsonProperty("seller_declared_breadth")
    private String sellerDeclaredBreadth = "";

    @JsonProperty("seller_declared_height")
    private String sellerDeclaredHeight = "";

    @JsonProperty("profiler_length")
    private String profilerLength = "";

    @JsonProperty("profiler_breadth")
    private String profilerBreadth = "";

    @JsonProperty("profiler_height")
    private String profilerHeight = "";

    @JsonProperty("source_state")
    private String sourceState = "";

    @JsonProperty("destination_state")
    private String destinationState = "";

    @JsonProperty("merchant_state")
    private String merchantState = "";

    @JsonProperty("accrual_party_id_to")
    private String accrualPartyIdTo;

    @JsonProperty("accrual_party_id_from")
    private String accrualPartyIdFrom = "EKL";

    @JsonProperty("accural_group")
    private String accuralGroup = "courier_service_group";

    @JsonProperty("m1_m2_flag")
    private String m1M2Flag = "M1";

    @JsonProperty("fwd_month")
    private Integer fwdMonth;

    @JsonProperty("pos_month")
    private Integer posMonth;

    @JsonProperty("rto_month")
    private Integer rtoMonth;

    @JsonProperty("rvp_month")
    private Integer rvpMonth;

    @JsonProperty("direct_delivered_month")
    private Integer directDeliveredMonth;

    @JsonProperty("error_message")
    private String errorMessage = "No Error";

    @JsonProperty("from_party_state_code")
    private String fromPartyStateCode = "defaultFrom";

    @JsonProperty("to_party_state_code")
    private String toPartyStateCode = "defaultTo";

    @JsonProperty("seller_volumetric_weight")
    private double sellerVolumetricWeight;

    @JsonProperty("profiler_volumetric_weight")
    private double profilerVolumetricWeight;

    @JsonProperty("billeable_weight")
    private double billableWeight;

    @JsonProperty("billable_weight_kgs")
    private String billableWeightKgs;

    @JsonProperty("exp_or_std")
    private String expOrStd;

    @JsonProperty("billing_zone")
    private String billingZone;

    @JsonProperty("ceil_weight")
    private String ceilWeight;

    @JsonProperty("zone_type")
    private String zoneType;

    @JsonProperty("entity_type")
    private String entityType;

    @JsonProperty("payment_mode")
    private String paymentMode;

    @JsonProperty("booking_date_time")
    private String bookingDateTime;

    @JsonProperty("value")
    private String value;

    @JsonProperty("flag")
    private String flag;

    @JsonProperty("freight_charge")
    private double freightCharge;

    @JsonProperty("cod_charges")
    private double codCharges;

    @JsonProperty("fuel_surcharge_std")
    private double fuelSurchargeStd = 0;

    @JsonProperty("fuel_surcharge_exp")
    private double fuelSurchargeExp = 0;

    @JsonProperty("fuel_surcharge_exceptional")
    private double fuelSurchargeExceptional = 0;

    @JsonProperty("pos_charge")
    private double posCharge;

    @JsonProperty("is_freight_found")
    private String isFreightFound;

    @JsonProperty("cod_revenue")
    private double codRevenue = 0;

    @JsonProperty("pos_revenue")
    private double posRevenue = 0;

    @JsonProperty("fwd_shipping_revenue")
    private double fwdShippingRevenue = 0;

    @JsonProperty("cod_pos_revenue")
    private double codPosRevenue = 0;

    @JsonProperty("rvp_revenue")
    private double rvpRevenue = 0;

    @JsonProperty("rto_revenue")
    private double rtoRevenue = 0;

    @JsonProperty("total_shipping_revenue")
    private double totalShippingRevenue;

    @JsonProperty("fuel_surcharges")
    private double fuelSurcharges = 0;

    @JsonProperty("total_amount_without_tax")
    private double totalAmountWithoutTax;

    @JsonProperty("total_revenue")
    private double totalRevenue;

    @JsonProperty("cgst_tax_rate")
    private String cgstTaxRate;

    @JsonProperty("sgst_tax_rate")
    private String sgstTaxRate;

    @JsonProperty("igst_tax_rate")
    private String igstTaxRate;

    @JsonProperty("cgst_total_amount")
    private double cgstTotalAmount = 0;

    @JsonProperty("sgst_total_amount")
    private double sgstTotalAmount = 0;

    @JsonProperty("igst_total_amount")
    private double igstTotalAmount = 0;

    @JsonProperty("total_tax")
    private double totalTax = 0;

    @JsonProperty("total_revenue_tax")
    private double totalRevenueTax = 0;

    @JsonProperty("total_amount_with_tax")
    private double totalAmountWithTax = 0;

    @JsonProperty("value_including_tax")
    private double valueIncludingTax;

    @JsonProperty("sac_code")
    private int sacCode = 0;

    @JsonProperty("fee_name")
    private int feeName = 0;

    @JsonProperty("is_diff_error")
    private String isDiffError;

    @JsonProperty("rvp_surcharges")
    private int rvpSurcharges=0;

    @JsonProperty("Shipping_discount")
    private double shippingDiscount=0;

    @JsonProperty("COD_discount")
    private double codDiscount=0;

    @JsonProperty("total_discount")
    private double totalDiscount=0;

    @JsonProperty("Net_shipping")
    private double netShipping;

    @JsonProperty("Net_COD")
    private double netCod;

    @JsonProperty("Net Revenue")
    private double netRevenue;

    @JsonProperty("Gross_shipping")
    private double grossShipping;

    @JsonProperty("Gross_COD_POS")
    private double grossCodPos;

    @JsonProperty("Gross_revenue")
    private double grossRevenue;

    @JsonProperty("bill_from_state")
    private String billFromState;

    public ProcessData() {
    }

    public ProcessData(ProcessData processData) {
        this.trackingId = processData.getTrackingId();
        this.merchantId = processData.getMerchantId();
        this.movementType = processData.getMovementType();
        this.clientId = processData.getClientId();
        this.pickupDate = processData.getPickupDate();
        this.mhInscanDate = processData.getMhInscanDate();
        this.shipmentType = processData.getShipmentType();
        this.shipmentStatus = processData.getShipmentStatus();
        this.shipmentStatusDate = processData.getShipmentStatusDate();
        this.zone = processData.getZone();
        this.paymentType = processData.getPaymentType();
        this.sellerDeadWeight = processData.getSellerDeadWeight();
        this.profilerDeadWeight = processData.getProfilerDeadWeight();
        this.sourcePincode = processData.getSourcePincode();
        this.destinationPincode = processData.getDestinationPincode();
        this.shipmentValue = processData.getShipmentValue();
        this.amountToCollect = processData.getAmountToCollect();
        this.sellerDeclaredLength = processData.getSellerDeclaredLength();
        this.sellerDeclaredBreadth = processData.getSellerDeclaredBreadth();
        this.sellerDeclaredHeight = processData.getSellerDeclaredHeight();
        this.profilerLength = processData.getProfilerLength();
        this.profilerBreadth = processData.getProfilerBreadth();
        this.profilerHeight = processData.getProfilerHeight();
        this.sourceState = processData.getSourceState();
        this.destinationState = processData.getDestinationState();
        this.merchantState = processData.getMerchantState();
        this.accrualPartyIdTo = processData.getAccrualPartyIdTo();
        this.accrualPartyIdFrom = processData.getAccrualPartyIdFrom();
        this.accuralGroup = processData.getAccuralGroup();
        this.m1M2Flag = processData.getM1M2Flag();
        this.fwdMonth = processData.getFwdMonth();
        this.posMonth = processData.getPosMonth();
        this.rtoMonth = processData.getRtoMonth();
        this.rvpMonth = processData.getRvpMonth();
        this.directDeliveredMonth = processData.getDirectDeliveredMonth();
        this.errorMessage = processData.getErrorMessage();
        this.fromPartyStateCode = processData.getFromPartyStateCode();
        this.toPartyStateCode = processData.getToPartyStateCode();
        this.sellerVolumetricWeight = processData.getSellerVolumetricWeight();
        this.profilerVolumetricWeight = processData.getProfilerVolumetricWeight();
        this.billableWeight = processData.getBillableWeight();
        this.billableWeightKgs = processData.getBillableWeightKgs();
        this.expOrStd = processData.getExpOrStd();
        this.billingZone = processData.getBillingZone();
        this.ceilWeight = processData.getCeilWeight();
        this.zoneType = processData.getZoneType();
        this.entityType = processData.getEntityType();
        this.paymentMode = processData.getPaymentMode();
        this.bookingDateTime = processData.getBookingDateTime();
        this.value = processData.getValue();
        this.flag = processData.getFlag();
        this.freightCharge = processData.getFreightCharge();
        this.codCharges = processData.getCodCharges();
        this.fuelSurchargeStd = processData.getFuelSurchargeStd();
        this.fuelSurchargeExp = processData.getFuelSurchargeExp();
        this.posCharge = processData.getPosCharge();
        this.isFreightFound = processData.getIsFreightFound();
        this.codRevenue = processData.getCodRevenue();
        this.posRevenue = processData.getPosRevenue();
        this.fwdShippingRevenue = processData.getFwdShippingRevenue();
        this.codPosRevenue = processData.getCodPosRevenue();
        this.rvpRevenue = processData.getRvpRevenue();
        this.rtoRevenue = processData.getRtoRevenue();
        this.totalShippingRevenue = processData.getTotalShippingRevenue();
        this.fuelSurcharges = processData.getFuelSurcharges();
        this.totalAmountWithoutTax = processData.getTotalAmountWithoutTax();
        this.totalRevenue = processData.getTotalRevenue();
        this.cgstTaxRate = processData.getCgstTaxRate();
        this.sgstTaxRate = processData.getSgstTaxRate();
        this.igstTaxRate = processData.getIgstTaxRate();
        this.cgstTotalAmount = processData.getCgstTotalAmount();
        this.sgstTotalAmount = processData.getSgstTotalAmount();
        this.igstTotalAmount = processData.getIgstTotalAmount();
        this.totalTax = processData.getTotalTax();
        this.totalRevenueTax = processData.getTotalRevenueTax();
        this.totalAmountWithTax = processData.getTotalAmountWithTax();
        this.valueIncludingTax = processData.getValueIncludingTax();
        this.sacCode = processData.getSacCode();
        this.feeName = processData.getFeeName();
        this.isDiffError = processData.getIsDiffError();
        this.fuelSurchargeExceptional = processData.getFuelSurchargeExceptional();
        this.sourceCity = processData.getSourceCity();
        this.destinationCity = processData.getDestinationCity();
        this.sourceZone = processData.getSourceZone();
        this.destinationZone = processData.getDestinationZone();
        this.sourceTier = processData.getSourceTier();
        this.destinationTier = processData.getDestinationTier();
        this.sourceZstate = processData.getSourceZstate();
        this.destinationZstate = processData.getDestinationZstate();
        this.rvpSurcharges=processData.getRvpSurcharges();
        this.shippingDiscount=processData.getShippingDiscount();
        this.codDiscount=processData.getCodDiscount();
        this.totalDiscount=processData.getTotalDiscount();
        this.netShipping=processData.getNetShipping();
        this.netCod=processData.getNetCod();
        this.netRevenue=processData.getNetRevenue();
        this.grossShipping=processData.getGrossShipping();
        this.grossCodPos=processData.getGrossCodPos();
        this.grossRevenue=processData.getGrossRevenue();
        this.billFromState=processData.getBillFromState();
    }

}
