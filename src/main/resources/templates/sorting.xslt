<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:ns2="http://o2.cz/cip/svc/customermgmt/billing/BillingProductManagement/1.0"
                xmlns:ns3="http://o2.cz/cip/svc/resourcemgmt/provisioning/ProvisioningProductManagement/1.0"
                xmlns:ns4="http://o2.cz/cip/svc/customermgmt/ordermgmt/CrmOrderNotificationHandling/2.0"
                xmlns:ns5="http://cz.o2.com/cip/svc/customermgmt/ordermgmt/GoodsOrdering/2.0"
        >
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <!--Vnorene sortovani itemData podle correlationId-->
    <xsl:template match="/ns5:CreateOrderRequest/ns5:requestBody/ns5:itemDataList">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns5:correlationId" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>


    <!--Vnorene sortovani podle productInstance a pak podle parameters-->
    <xsl:template match="/ns2:ManageProductRequest/ns2:requestBody/ns2:products/ns2:product/ns2:parameters">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns2:key" />
                <xsl:sort lang="en" select="ns2:value" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="/ns2:ManageProductRequest/ns2:requestBody/ns2:products">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns2:productCode" />
                <xsl:sort lang="en" select="ns2:productInstance" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/ns2:ManageProductRequest/ns2:requestBody/ns2:tariffSpace/ns2:parameters">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns2:key" />
                <xsl:sort lang="en" select="ns2:value" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/ns3:ManageProductRequest/ns3:requestBody/ns3:products/ns3:product/ns3:links">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns3:targetInstance" />
                <xsl:sort lang="en" select="ns3:relationType" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/ns2:ManageProductRequest/ns2:requestBody/ns2:products/ns2:product/ns2:links">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns2:targetInstance" />
                <xsl:sort lang="en" select="ns2:relationType" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <!--Vnorene sortovani podle techOrder,items a pak podle attribute-->
    <xsl:template match="/ns4:UpdateOrderRequest/ns4:requestBody/ns4:techOrders/ns4:techOrder/ns4:items/ns4:item/ns4:attributes">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns4:key" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/ns4:UpdateOrderRequest/ns4:requestBody/ns4:techOrders/ns4:techOrder/ns4:items">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns4:orderItemId" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/ns4:UpdateOrderRequest/ns4:requestBody/ns4:techOrders">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns4:header/ns4:techOrderId" />
                <xsl:sort lang="en" select="ns4:header/ns4:techOrderType" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <!--Vnorene sortovani podle  items a pak podle attribute-->
    <xsl:template match="/ns4:UpdateTechnicalOrderRequest/ns4:requestBody/ns4:items/ns4:item/ns4:attributes">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns4:key" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/ns4:UpdateTechnicalOrderRequest/ns4:requestBody/ns4:items">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns4:orderItemId" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <!--Vnorene sortovani podle product a pak podle parameters-->
    <xsl:template match="/ns3:ManageProductRequest/ns3:requestBody/ns3:products/ns3:product/ns3:parameters">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns3:key" />
                <xsl:sort lang="en" select="ns3:value" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/ns3:ManageProductRequest/ns3:requestBody/ns3:products">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns3:productCode" />
                <xsl:sort lang="en" select="ns3:productInstanceId" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/ns4:UpdateTechnicalOrderRequest/ns4:requestBody/ns4:items">
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort lang="en" select="ns4:orderItemId" />
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>

        <!--
        <xsl:sort data-type="text" select="ns2:key" order="ascending" />
        -->
