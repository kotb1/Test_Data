package org.example.Creations;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * Sends a batch of work orders to the WFM server for performance testing.
 * <p>
 * There are 6 order types: 4 installation (REST) and 2 DATA maintenance (SOAP).
 * The requested number of orders is split over the CSV files by FILE_WEIGHTS, and each type is sized to fit its files.
 */
public class Create
{
    // Request templates, one per order type. IDs inside them are replaced with random ones before each send.
    //String AVReSplitReserve_body= "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><wfmCreateOrder><OMOrderID>17833295100250176</OMOrderID><OMOrderType>1001</OMOrderType><cityCode>2</cityCode><ServiceNo>11111112</ServiceNo><ExchID>17851</ExchID><CustomerSegment>VIP</CustomerSegment><additionalInfoList><attCode>COMOrderID</attCode><attValue>56056055573449</attValue></additionalInfoList><additionalInfoList><attCode>COMOrderType</attCode><attValue>1001</attValue></additionalInfoList><additionalInfoList><attCode>OMOrderType ID</attCode><attValue>1011</attValue></additionalInfoList><additionalInfoList><attCode>CustomerName</attCode><attValue>MohamedHassan</attValue></additionalInfoList><additionalInfoList><attCode>AccountNo</attCode><attValue>51204902@tedata.net.eg</attValue></additionalInfoList><additionalInfoList><attCode>NumberRequestedDevices</attCode><attValue>1</attValue></additionalInfoList><additionalInfoList><attCode>CompoundID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>VillaNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>InstallationAddress</attCode><attValue>Cairo,Kilo 28,Kilo 28,1,1,1</attValue></additionalInfoList><additionalInfoList><attCode>secondFTTHPhoneNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CustomerMobileNo</attCode><attValue>1553132660</attValue></additionalInfoList><additionalInfoList><attCode>PackageContent</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalWorkOrderID</attCode><attValue>15894</attValue></additionalInfoList><additionalInfoList><attCode>AccountType</attCode><attValue>Prepaid</attValue></additionalInfoList><additionalInfoList><attCode>CustomerLevel</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ContractedServiceSpeed</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>PortType</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>UNMSframeNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>LocalLoopFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>CircuitNumber</attCode><attValue>FBB236501418</attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalCancellationWOID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>OptionPackFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FamilyFilter (true /false)</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>ReplacementFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>ONTID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CardNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>PortNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>LastMileFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>BoxNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CompoundDirectSupportFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Sector</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>SupportedByTEFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FCCExchangeCode</attCode><attValue>MBKGZ</attValue></additionalInfoList><additionalInfoList><attCode>CabinNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Have_IPTV_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Have_IPPhone_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Migration Flag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>Other ISP flag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>ISP ID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ISP Name</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Old Cabin</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>svFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>FCC error code</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FVCustomer no</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Reserve_ID</attCode><attValue>OSS_20260706125030473955</attValue></additionalInfoList><additionalInfoList><attCode>Service_Type</attCode><attValue>ADSL</attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_PORT</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_ODF</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_ODF</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_PORT</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Convergent_Flag</attCode><attValue></attValue></additionalInfoList><NewDevices><NewDevice><InstanceID></InstanceID><SerialNo></SerialNo><Model></Model><Type></Type><Mac></Mac></NewDevice></NewDevices></wfmCreateOrder>";
    //String ADSL_Un_Splitting = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><wfmCreateOrder><OMOrderID>17830008900900134</OMOrderID><OMOrderType>1002</OMOrderType><cityCode>2</cityCode><ServiceNo>11111112</ServiceNo><ExchID>17851</ExchID><CustomerSegment>Welcome Core</CustomerSegment><additionalInfoList><attCode>COMOrderID</attCode><attValue>56056055495867</attValue></additionalInfoList><additionalInfoList><attCode>COMOrderType</attCode><attValue>1002</attValue></additionalInfoList><additionalInfoList><attCode>OMOrderType ID</attCode><attValue>1002</attValue></additionalInfoList><additionalInfoList><attCode>CustomerName</attCode><attValue>Mageddays</attValue></additionalInfoList><additionalInfoList><attCode>AccountNo</attCode><attValue>51163718@tedata.net.eg</attValue></additionalInfoList><additionalInfoList><attCode>NumberRequestedDevices</attCode><attValue>1</attValue></additionalInfoList><additionalInfoList><attCode>CompoundID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>VillaNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>InstallationAddress</attCode><attValue>Cairo,Kilo 28,Kilo 28,1,1,1</attValue></additionalInfoList><additionalInfoList><attCode>secondFTTHPhoneNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CustomerMobileNo</attCode><attValue>NeaD+Cw7rSsEpiq/QLMGYg==</attValue></additionalInfoList><additionalInfoList><attCode>PackageContent</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalWorkOrderID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>AccountType</attCode><attValue>Prepaid</attValue></additionalInfoList><additionalInfoList><attCode>CustomerLevel</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ContractedServiceSpeed</attCode><attValue>153600/3</attValue></additionalInfoList><additionalInfoList><attCode>PortType</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>UNMSframeNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>LocalLoopFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>CircuitNumber</attCode><attValue>FBB236500539</attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalCancellationWOID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>OptionPackFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>LastMileFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FamilyFilter (true /false)</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>ReplacementFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>ONTID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CardNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>PortNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>BoxNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CompoundDirectSupportFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Sector</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>SupportedByTEFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FCCExchangeCode</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>CabinNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Have_IPTV_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Have_IPPhone_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>svFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>FCC error code</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Reserve_ID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Service_Type</attCode><attValue>VDSL</attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_PORT</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_ODF</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_ODF</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_PORT</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Convergent_Flag</attCode><attValue></attValue></additionalInfoList><NewDevices><NewDevice><InstanceID></InstanceID><SerialNo></SerialNo><Model></Model><Type></Type><Mac></Mac></NewDevice></NewDevices></wfmCreateOrder>";
    String ADSL_VDSL_Splitting = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><wfmCreateOrder><OMOrderID>17826373500830103</OMOrderID><OMOrderType>1001</OMOrderType><cityCode>2</cityCode><ServiceNo>11111112</ServiceNo><ExchID>17851</ExchID><CustomerSegment>Welcome</CustomerSegment><additionalInfoList><attCode>COMOrderID</attCode><attValue>56056055431513</attValue></additionalInfoList><additionalInfoList><attCode>COMOrderType</attCode><attValue>1001</attValue></additionalInfoList><additionalInfoList><attCode>OMOrderType ID</attCode><attValue>1001</attValue></additionalInfoList><additionalInfoList><attCode>CustomerName</attCode><attValue>MohamedSakr</attValue></additionalInfoList><additionalInfoList><attCode>AccountNo</attCode><attValue>51201222@tedata.net.eg</attValue></additionalInfoList><additionalInfoList><attCode>NumberRequestedDevices</attCode><attValue>1</attValue></additionalInfoList><additionalInfoList><attCode>CompoundID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>VillaNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>InstallationAddress</attCode><attValue>A-UAT,A-UAT2,A-UAT3,1,1</attValue></additionalInfoList><additionalInfoList><attCode>secondFTTHPhoneNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CustomerMobileNo</attCode><attValue>a1IGzkvDLoaBSjYy+D4M9w==</attValue></additionalInfoList><additionalInfoList><attCode>PackageContent</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalWorkOrderID</attCode><attValue>15746</attValue></additionalInfoList><additionalInfoList><attCode>AccountType</attCode><attValue>Prepaid</attValue></additionalInfoList><additionalInfoList><attCode>CustomerLevel</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ContractedServiceSpeed</attCode><attValue>30720/10</attValue></additionalInfoList><additionalInfoList><attCode>PortType</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>UNMSframeNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>LocalLoopFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>CircuitNumber</attCode><attValue>FBB236500500</attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalCancellationWOID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>OptionPackFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>LastMileFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FamilyFilter (true /false)</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>ReplacementFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>ONTID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CardNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>PortNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>BoxNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CompoundDirectSupportFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Sector</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>SupportedByTEFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FCCExchangeCode</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>CabinNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Have_IPTV_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Have_IPPhone_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>svFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>FCC error code</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Reserve_ID</attCode><attValue>OSS_20260628120430481159</attValue></additionalInfoList><additionalInfoList><attCode>Service_Type</attCode><attValue>ADSL</attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_PORT</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_ODF</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_ODF</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_PORT</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Convergent_Flag</attCode><attValue></attValue></additionalInfoList><NewDevices><NewDevice><InstanceID></InstanceID><SerialNo></SerialNo><Model></Model><Type></Type><Mac></Mac></NewDevice></NewDevices></wfmCreateOrder>";
    String ADSL_Change_to_VDSL = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><wfmCreateOrder><OMOrderID>17647657100250163</OMOrderID><OMOrderType>1004</OMOrderType><cityCode>2</cityCode><ServiceNo>11111112</ServiceNo><ExchID>17851</ExchID><CustomerSegment>Welcome</CustomerSegment><additionalInfoList><attCode>COMOrderID</attCode><attValue>56056052391518</attValue></additionalInfoList><additionalInfoList><attCode>COMOrderType</attCode><attValue>1002</attValue></additionalInfoList><additionalInfoList><attCode>OMOrderType ID</attCode><attValue>1001</attValue></additionalInfoList><additionalInfoList><attCode>CustomerName</attCode><attValue>MohamedSakr</attValue></additionalInfoList><additionalInfoList><attCode>AccountNo</attCode><attValue>51134731@tedata.net.eg</attValue></additionalInfoList><additionalInfoList><attCode>NumberRequestedDevices</attCode><attValue>1</attValue></additionalInfoList><additionalInfoList><attCode>CompoundID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>VillaNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>InstallationAddress</attCode><attValue>Cairo,Kilo 28,Kilo 28,Shebin Qanater,1,1</attValue></additionalInfoList><additionalInfoList><attCode>secondFTTHPhoneNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CustomerMobileNo</attCode><attValue>a1IGzkvDLoaBSjYy+D4M9w==</attValue></additionalInfoList><additionalInfoList><attCode>PackageContent</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalWorkOrderID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>AccountType</attCode><attValue>Prepaid</attValue></additionalInfoList><additionalInfoList><attCode>CustomerLevel</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ContractedServiceSpeed</attCode><attValue>30720/3</attValue></additionalInfoList><additionalInfoList><attCode>PortType</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>UNMSframeNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>LocalLoopFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>CircuitNumber</attCode><attValue>FBB236500249</attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalCancellationWOID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>OptionPackFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>LastMileFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FamilyFilter (true /false)</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>ReplacementFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>ONTID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CardNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>PortNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>BoxNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CompoundDirectSupportFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Sector</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>SupportedByTEFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FCCExchangeCode</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>CabinNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Have_IPTV_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Have_IPPhone_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>svFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>FCC error code</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Reserve_ID</attCode><attValue>OSS_20251203144230135135</attValue></additionalInfoList><additionalInfoList><attCode>Service_Type</attCode><attValue>VDSL</attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_PORT</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_ODF</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_ODF</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_PORT</attCode><attValue></attValue></additionalInfoList><NewDevices><NewDevice><InstanceID></InstanceID><SerialNo></SerialNo><Model></Model><Type></Type><Mac></Mac></NewDevice></NewDevices></wfmCreateOrder>";
    String Data_Down = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:fcc=\"http://FCCHiveWS/\"><SOAP-ENV:Header/><SOAP-ENV:Body><fcc:CreateWoFCC><WorkOrderInfo><addList><attCode>Name</attCode><attValue>TEST</attValue></addList><addList><attCode>Address</attCode><attValue>Detailed Address:[Shebin Qanater], P.O.Box:[],  Postal Code:[],  Block:[], House No.:[1], Room No.:[], Landmark:[1], Area:[Kilo 28], City:[Kilo 28], Governorate:[Cairo]</attValue></addList><addList><attCode>City_Code</attCode><attValue>2</attValue></addList><addList><attCode>Tel_Num</attCode><attValue>36500323</attValue></addList><addList><attCode>LINE_ID</attCode><attValue>51135316</attValue></addList><addList><attCode>SERVICE_TYPE</attCode><attValue>ESL</attValue></addList><addList><attCode>PORT_TYPE</attCode><attValue>N/A</attValue></addList><addList><attCode>EXCH_CODE</attCode><attValue>MBKGZ</attValue></addList><addList><attCode>SWITCH_CODE</attCode><attValue>MBK</attValue></addList><addList><attCode>LINE_TYPE</attCode><attValue>2</attValue></addList><addList><attCode>MDF_VERT/T</attCode><attValue>100/300</attValue></addList><addList><attCode>CABLE_NO/T</attCode><attValue>100/300</attValue></addList><addList><attCode>CABINET_NO</attCode><attValue>B7</attValue></addList><addList><attCode>PBLOCK_NO/CAB_IN</attCode><attValue>1/300</attValue></addList><addList><attCode>SBLOCK_NO/CAB_OUT</attCode><attValue>1/105</attValue></addList><addList><attCode>DP/T</attCode><attValue>1/105</attValue></addList><addList><attCode>PORT</attCode><attValue></attValue></addList><addList><attCode>SBLOCK_NO/FIBER_OUT</attCode><attValue>N/A</attValue></addList><addList><attCode>MDF_Ex_Side</attCode><attValue>N/A</attValue></addList><addList><attCode>Line_Eq_No</attCode><attValue>N/A</attValue></addList><addList><attCode>IDU_NO</attCode><attValue>N/A</attValue></addList><addList><attCode>ODU_NO</attCode><attValue>N/A</attValue></addList><addList><attCode>POP</attCode><attValue>N/A</attValue></addList><addList><attCode>ADSL_FRAME</attCode><attValue>N/A</attValue></addList><addList><attCode>DSLAM_Type</attCode><attValue>N/A</attValue></addList><addList><attCode>Follow UP</attCode><attValue>01227789631</attValue></addList><addList><attCode>Sec_Tel</attCode><attValue>01227789163</attValue></addList><cityCode>2</cityCode><complainNo>1307325600306</complainNo><dpLongitude>31.029</dpLongitude><dpLatitude>30.082</dpLatitude><exchCode>MBKGZ</exchCode><initiator>FCC</initiator><notes>تعديل رقم المتابعة من  بواسطة</notes><priCode>عادي</priCode><detractorFlag>1</detractorFlag><requestType>Data down</requestType><telNo>11111112</telNo></WorkOrderInfo></fcc:CreateWoFCC></SOAP-ENV:Body></SOAP-ENV:Envelope>";
    String Physical_Instability = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:fcc=\"http://FCCHiveWS/\"><SOAP-ENV:Header/><SOAP-ENV:Body><fcc:CreateWoFCC><WorkOrderInfo><addList><attCode>Name</attCode><attValue>TEST</attValue></addList><addList><attCode>Address</attCode><attValue>Detailed Address:[Shebin Qanater], P.O.Box:[],  Postal Code:[],  Block:[], House No.:[1], Room No.:[], Landmark:[1], Area:[Kilo 28], City:[Kilo 28], Governorate:[Cairo]</attValue></addList><addList><attCode>City_Code</attCode><attValue>2</attValue></addList><addList><attCode>Tel_Num</attCode><attValue>36500323</attValue></addList><addList><attCode>LINE_ID</attCode><attValue>51135316</attValue></addList><addList><attCode>SERVICE_TYPE</attCode><attValue>ESL</attValue></addList><addList><attCode>PORT_TYPE</attCode><attValue>N/A</attValue></addList><addList><attCode>EXCH_CODE</attCode><attValue>MBKGZ</attValue></addList><addList><attCode>SWITCH_CODE</attCode><attValue>MBK</attValue></addList><addList><attCode>LINE_TYPE</attCode><attValue>2</attValue></addList><addList><attCode>MDF_VERT/T</attCode><attValue>100/300</attValue></addList><addList><attCode>CABLE_NO/T</attCode><attValue>100/300</attValue></addList><addList><attCode>CABINET_NO</attCode><attValue>B7</attValue></addList><addList><attCode>PBLOCK_NO/CAB_IN</attCode><attValue>1/300</attValue></addList><addList><attCode>SBLOCK_NO/CAB_OUT</attCode><attValue>1/105</attValue></addList><addList><attCode>DP/T</attCode><attValue>1/105</attValue></addList><addList><attCode>PORT</attCode><attValue></attValue></addList><addList><attCode>SBLOCK_NO/FIBER_OUT</attCode><attValue>N/A</attValue></addList><addList><attCode>MDF_Ex_Side</attCode><attValue>N/A</attValue></addList><addList><attCode>Line_Eq_No</attCode><attValue>N/A</attValue></addList><addList><attCode>IDU_NO</attCode><attValue>N/A</attValue></addList><addList><attCode>ODU_NO</attCode><attValue>N/A</attValue></addList><addList><attCode>POP</attCode><attValue>N/A</attValue></addList><addList><attCode>ADSL_FRAME</attCode><attValue>N/A</attValue></addList><addList><attCode>DSLAM_Type</attCode><attValue>N/A</attValue></addList><addList><attCode>Follow UP</attCode><attValue>01227789631</attValue></addList><addList><attCode>Sec_Tel</attCode><attValue>01227789163</attValue></addList><cityCode>2</cityCode><complainNo>1307325600306</complainNo><dpLongitude>31.029</dpLongitude><dpLatitude>30.082</dpLatitude><exchCode>MBKGZ</exchCode><initiator>FCC</initiator><notes>تعديل رقم المتابعة من  بواسطة</notes><priCode>عادي</priCode><detractorFlag>1</detractorFlag><requestType>Physical Instability</requestType><telNo>11111112</telNo></WorkOrderInfo></fcc:CreateWoFCC></SOAP-ENV:Body></SOAP-ENV:Envelope>";
    String FTTHUnRchReSurvey="<?xml version=\"1.0\" encoding=\"UTF-8\" ?><wfmCreateOrder><OMOrderID>17747766105550162</OMOrderID><OMOrderType>1023</OMOrderType><cityCode>2</cityCode><ServiceNo>11111112</ServiceNo><ExchID>502</ExchID><CustomerSegment>Welcome</CustomerSegment><additionalInfoList><attCode>COMOrderID</attCode><attValue>56056054036920</attValue></additionalInfoList><additionalInfoList><attCode>COMOrderType</attCode><attValue>1022</attValue></additionalInfoList><additionalInfoList><attCode>OMOrderType ID</attCode><attValue>1017</attValue></additionalInfoList><additionalInfoList><attCode>CustomerName</attCode><attValue>MohamedSakr</attValue></additionalInfoList><additionalInfoList><attCode>AccountNo</attCode><attValue>51146563@tedata.net.eg</attValue></additionalInfoList><additionalInfoList><attCode>NumberRequestedDevices</attCode><attValue>1</attValue></additionalInfoList><additionalInfoList><attCode>CompoundID</attCode><attValue>118</attValue></additionalInfoList><additionalInfoList><attCode>VillaNo</attCode><attValue>2</attValue></additionalInfoList><additionalInfoList><attCode>InstallationAddress</attCode><attValue>Cairo,Kilo 28,Kilo 28,1,2,1</attValue></additionalInfoList><additionalInfoList><attCode>secondFTTHPhoneNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CustomerMobileNo</attCode><attValue>1507007771</attValue></additionalInfoList><additionalInfoList><attCode>PackageContent</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalWorkOrderID</attCode><attValue>310</attValue></additionalInfoList><additionalInfoList><attCode>AccountType</attCode><attValue>Postpaid</attValue></additionalInfoList><additionalInfoList><attCode>CustomerLevel</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ContractedServiceSpeed</attCode><attValue>30720/1.2</attValue></additionalInfoList><additionalInfoList><attCode>PortType</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>UNMSframeNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>LocalLoopFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>CircuitNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalCancellationWOID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>OptionPackFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>LastMileFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FamilyFilter (true /false)</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>ReplacementFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>ONTID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CardNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>PortNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>BoxNumber</attCode><attValue>23</attValue></additionalInfoList><additionalInfoList><attCode>CompoundDirectSupportFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Sector</attCode><attValue>قطاع الجيزة</attValue></additionalInfoList><additionalInfoList><attCode>SupportedByTEFlag</attCode><attValue>1</attValue></additionalInfoList><additionalInfoList><attCode>FCCExchangeCode</attCode><attValue>MBKGZ</attValue></additionalInfoList><additionalInfoList><attCode>CabinNumber</attCode><attValue>SV CABINET 502</attValue></additionalInfoList><additionalInfoList><attCode>Have_IPTV_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Have_IPPhone_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Migration Flag</attCode><attValue>Y</attValue></additionalInfoList><additionalInfoList><attCode>Other ISP flag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>ISP ID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ISP Name</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Old Cabin</attCode><attValue>MSAN</attValue></additionalInfoList><additionalInfoList><attCode>svFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>FCC error code</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FVCustomer no</attCode><attValue>51146563</attValue></additionalInfoList><additionalInfoList><attCode>Reserve_ID</attCode><attValue>OSS_20260924112130226403</attValue></additionalInfoList><additionalInfoList><attCode>Service_Type</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_PORT</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_ODF</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_ODF</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_PORT</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>FVNO Flag</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>OperatorName</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Convergent_Flag</attCode><attValue></attValue></additionalInfoList><NewDevices><NewDevice><InstanceID>8087689610004319425</InstanceID><SerialNo></SerialNo><Model></Model><Type>ONT</Type><Mac></Mac></NewDevice></NewDevices></wfmCreateOrder>";
    String FTTHNewSubVS = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><wfmCreateOrder><OMOrderID>17917926600220120</OMOrderID><OMOrderType>1026</OMOrderType><cityCode>2</cityCode><ServiceNo>11111112</ServiceNo><ExchID>502</ExchID><CustomerSegment>Welcome</CustomerSegment><additionalInfoList><attCode>COMOrderID</attCode><attValue>56056057031041</attValue></additionalInfoList><additionalInfoList><attCode>COMOrderType</attCode><attValue>1024</attValue></additionalInfoList><additionalInfoList><attCode>OMOrderType ID</attCode><attValue>1004</attValue></additionalInfoList><additionalInfoList><attCode>CustomerName</attCode><attValue>RamyEldeep</attValue></additionalInfoList><additionalInfoList><attCode>AccountNo</attCode><attValue>51238841@tedata.net.eg</attValue></additionalInfoList><additionalInfoList><attCode>NumberRequestedDevices</attCode><attValue>1</attValue></additionalInfoList><additionalInfoList><attCode>CompoundID</attCode><attValue>676</attValue></additionalInfoList><additionalInfoList><attCode>VillaNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>InstallationAddress</attCode><attValue>Cairo,Kilo 28,Kilo 28,Shebin Qanater,1,1</attValue></additionalInfoList><additionalInfoList><attCode>secondFTTHPhoneNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CustomerMobileNo</attCode><attValue>1555793275</attValue></additionalInfoList><additionalInfoList><attCode>PackageContent</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalWorkOrderID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>AccountType</attCode><attValue>Postpaid</attValue></additionalInfoList><additionalInfoList><attCode>CustomerLevel</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ContractedServiceSpeed</attCode><attValue>51200/2</attValue></additionalInfoList><additionalInfoList><attCode>PortType</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>UNMSframeNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>LocalLoopFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>CircuitNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalCancellationWOID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>OptionPackFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>LastMileFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FamilyFilter (true /false)</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>ReplacementFlag</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ONTID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CardNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>PortNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>BoxNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CompoundDirectSupportFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Sector</attCode><attValue>قطاع الجيزة</attValue></additionalInfoList><additionalInfoList><attCode>SupportedByTEFlag</attCode><attValue>1</attValue></additionalInfoList><additionalInfoList><attCode>FCCExchangeCode</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>CabinNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Have_IPTV_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Have_IPPhone_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Migration Flag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>Other ISP flag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>ISP ID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ISP Name</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Old Cabin</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>svFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>FCC error code</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Reserve_ID</attCode><attValue>CRM_20261006074434114482</attValue></additionalInfoList><additionalInfoList><attCode>Service_Type</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_PORT</attCode><attValue>13</attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_ODF</attCode><attValue>R1/ODF1/C2</attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_ODF</attCode><attValue>R1/ODF1/C2</attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_PORT</attCode><attValue>2</attValue></additionalInfoList><additionalInfoList><attCode>Convergent_Flag</attCode><attValue></attValue></additionalInfoList><NewDevices><NewDevice><InstanceID>8087689610004711642</InstanceID><SerialNo></SerialNo><Model></Model><Type>ONT</Type><Mac></Mac></NewDevice></NewDevices></wfmCreateOrder>";
    //String FTTHMigrationSurvey = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><wfmCreateOrder><OMOrderID>17911022900660196</OMOrderID><OMOrderType>1023</OMOrderType><cityCode>2</cityCode><ServiceNo>11111112</ServiceNo><ExchID>502</ExchID><CustomerSegment>Welcome</CustomerSegment><additionalInfoList><attCode>COMOrderID</attCode><attValue>56056056974345</attValue></additionalInfoList><additionalInfoList><attCode>COMOrderType</attCode><attValue>1022</attValue></additionalInfoList><additionalInfoList><attCode>OMOrderType ID</attCode><attValue>1004</attValue></additionalInfoList><additionalInfoList><attCode>CustomerName</attCode><attValue>MohamedSakr</attValue></additionalInfoList><additionalInfoList><attCode>AccountNo</attCode><attValue>51224465@tedata.net.eg</attValue></additionalInfoList><additionalInfoList><attCode>NumberRequestedDevices</attCode><attValue>1</attValue></additionalInfoList><additionalInfoList><attCode>CompoundID</attCode><attValue>503</attValue></additionalInfoList><additionalInfoList><attCode>VillaNo</attCode><attValue>659</attValue></additionalInfoList><additionalInfoList><attCode>InstallationAddress</attCode><attValue>Cairo,Kilo 28,Kilo 28,1,1,1</attValue></additionalInfoList><additionalInfoList><attCode>secondFTTHPhoneNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CustomerMobileNo</attCode><attValue>1507007771</attValue></additionalInfoList><additionalInfoList><attCode>PackageContent</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalWorkOrderID</attCode><attValue>16835</attValue></additionalInfoList><additionalInfoList><attCode>AccountType</attCode><attValue>Prepaid</attValue></additionalInfoList><additionalInfoList><attCode>CustomerLevel</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ContractedServiceSpeed</attCode><attValue>30720/1.2</attValue></additionalInfoList><additionalInfoList><attCode>PortType</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>UNMSframeNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>LocalLoopFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>CircuitNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ADSLportalCancellationWOID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>OptionPackFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>LastMileFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FamilyFilter (true /false)</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>ReplacementFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>ONTID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CardNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>PortNo</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>BoxNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>CompoundDirectSupportFlag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Sector</attCode><attValue>قطاع الجيزة</attValue></additionalInfoList><additionalInfoList><attCode>SupportedByTEFlag</attCode><attValue>1</attValue></additionalInfoList><additionalInfoList><attCode>FCCExchangeCode</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>CabinNumber</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Have_IPTV_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Have_IPPhone_Flag</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>Migration Flag</attCode><attValue>Y</attValue></additionalInfoList><additionalInfoList><attCode>Other ISP flag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>ISP ID</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>ISP Name</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Old Cabin</attCode><attValue>MSAN</attValue></additionalInfoList><additionalInfoList><attCode>svFlag</attCode><attValue>N</attValue></additionalInfoList><additionalInfoList><attCode>FCC error code</attCode><attValue>0</attValue></additionalInfoList><additionalInfoList><attCode>FVCustomer no</attCode><attValue>51224465</attValue></additionalInfoList><additionalInfoList><attCode>Reserve_ID</attCode><attValue>OSS_20261004114530407396</attValue></additionalInfoList><additionalInfoList><attCode>Service_Type</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_PORT</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_ACTIVE_ODF</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_ODF</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>MMR_PASSIVE_PORT</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>FVNO Flag</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>OperatorName</attCode><attValue></attValue></additionalInfoList><additionalInfoList><attCode>Convergent_Flag</attCode><attValue></attValue></additionalInfoList><NewDevices><NewDevice><InstanceID>8087689610004707927</InstanceID><SerialNo></SerialNo><Model></Model><Type>ONT</Type><Mac></Mac></NewDevice></NewDevices></wfmCreateOrder>";
    //String FTTHDataVoiceDown = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:fcc=\"http://fccwfminteg/\"><SOAP-ENV:Header/><SOAP-ENV:Body><fcc:CreateWo><WorkOrderInfo><addList><attCode>CustomerName</attCode><attValue>UAT TEST</attValue></addList><addList><attCode>Address</attCode><attValue>Detailed Address:[1], P.O.Box:[],  Postal Code:[],  Block:[], House No.:[1], Room No.:[], Landmark:[1], Area:[Kilo 28], City:[Kilo 28], Governorate:[Cairo]</attValue></addList><addList><attCode>Mob_Num</attCode><attValue>01020095690</attValue></addList><addList><attCode>City_Code</attCode><attValue>2</attValue></addList><addList><attCode>Tel_Num</attCode><attValue>36500304</attValue></addList><addList><attCode>Sec_Tel</attCode><attValue>01020095690</attValue></addList><addList><attCode>ActivationDate</attCode><attValue>03-08-2026 10:43</attValue></addList><addList><attCode>LINE_ID</attCode><attValue>ORANGEDATA_0236500304</attValue></addList><addList><attCode>MSAN_Code</attCode><attValue>04-3-69-952</attValue></addList><addList><attCode>MSAN_IP</attCode><attValue>10.231.0.2</attValue></addList><addList><attCode>MSAN_Vendor_IP</attCode><attValue>1</attValue></addList><addList><attCode>MSAN_Port_No</attCode><attValue>0/14/5</attValue></addList><addList><attCode>ONTChannel</attCode><attValue>35</attValue></addList><addList><attCode>ONTSerialNo</attCode><attValue>48575443FFFF6532</attValue></addList><addList><attCode>DataPort</attCode><attValue>5</attValue></addList><addList><attCode>VoicePort</attCode><attValue>5</attValue></addList><addList><attCode>DataSlot</attCode><attValue>14</attValue></addList><addList><attCode>VoiceSlot</attCode><attValue>14</attValue></addList><addList><attCode>SHELF</attCode><attValue>0</attValue></addList><addList><attCode>FRAME</attCode><attValue>1</attValue></addList><addList><attCode>OLTName</attCode><attValue>04-3-69-952_M01H_SMRTVLG_R05J</attValue></addList><addList><attCode>ActiveODF</attCode><attValue>test_MMR</attValue></addList><addList><attCode>PassiveODF</attCode><attValue>1</attValue></addList><addList><attCode>EXCH_CODE</attCode><attValue>MBKGZ</attValue></addList><addList><attCode>CABINET_NO</attCode><attValue>SV CABINET 502</attValue></addList><addList><attCode>BoxNo</attCode><attValue>25</attValue></addList><addList><attCode>Splitter</attCode><attValue>25</attValue></addList><addList><attCode>POP</attCode><attValue>17851</attValue></addList><addList><attCode>POPName</attCode><attValue>Smart Village Cabinet 952</attValue></addList><cityCode>2</cityCode><complainNo>23650030402</complainNo><cabinetNo>SV CABINET 502</cabinetNo><dpLongitude/><dpLatitude/><exchCode>MBKGZ</exchCode><initiator>FCC</initiator><notes>تعديل رقم المتابعة من  بواسطة </notes><priCode>Normal</priCode><detractorFlag>0</detractorFlag><requestType>FTTHDataandVoiceDown</requestType><telNo>11111112</telNo></WorkOrderInfo></fcc:CreateWo></SOAP-ENV:Body></SOAP-ENV:Envelope>";
    //String FTTHWrongCardPort = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:fcc=\"http://fccwfminteg/\"><SOAP-ENV:Header/><SOAP-ENV:Body><fcc:CreateWo><WorkOrderInfo><addList><attCode>Name</attCode><attValue>UAT TEST</attValue></addList><addList><attCode>Address</attCode><attValue>Detailed Address:[1], P.O.Box:[],  Postal Code:[],  Block:[], House No.:[1], Room No.:[], Landmark:[1], Area:[Kilo 28], City:[Kilo 28], Governorate:[Cairo]</attValue></addList><addList><attCode>Mob_Num</attCode><attValue>01020095690</attValue></addList><addList><attCode>City_Code</attCode><attValue>2</attValue></addList><addList><attCode>Tel_Num</attCode><attValue>36500304</attValue></addList><addList><attCode>Sec_Tel</attCode><attValue>01020095690</attValue></addList><addList><attCode>ActivationDate</attCode><attValue>03-08-2026 10:43</attValue></addList><addList><attCode>LINE_ID</attCode><attValue>ORANGEDATA_0236500304</attValue></addList><addList><attCode>MSAN_Code</attCode><attValue>04-3-69-952</attValue></addList><addList><attCode>MSAN_IP</attCode><attValue>10.231.0.2</attValue></addList><addList><attCode>MSAN_Vendor_IP</attCode><attValue>1</attValue></addList><addList><attCode>MSAN_Port_No</attCode><attValue>0/14/5</attValue></addList><addList><attCode>ONTChannel</attCode><attValue>35</attValue></addList><addList><attCode>ONTSerialNo</attCode><attValue>48575443FFFF6532</attValue></addList><addList><attCode>DataPort</attCode><attValue>5</attValue></addList><addList><attCode>VoicePort</attCode><attValue>5</attValue></addList><addList><attCode>DataSlot</attCode><attValue>14</attValue></addList><addList><attCode>VoiceSlot</attCode><attValue>14</attValue></addList><addList><attCode>SHELF</attCode><attValue>0</attValue></addList><addList><attCode>FRAME</attCode><attValue>1</attValue></addList><addList><attCode>OLTName</attCode><attValue>04-3-69-952_M01H_SMRTVLG_R05J</attValue></addList><addList><attCode>ActiveODF</attCode><attValue>test_MMR</attValue></addList><addList><attCode>PassiveODF</attCode><attValue>1</attValue></addList><addList><attCode>EXCH_CODE</attCode><attValue>MBKGZ</attValue></addList><addList><attCode>CABINET_NO</attCode><attValue>SV CABINET 502</attValue></addList><addList><attCode>BoxNo</attCode><attValue>25</attValue></addList><addList><attCode>Splitter</attCode><attValue>25</attValue></addList><addList><attCode>POP</attCode><attValue>17851</attValue></addList><addList><attCode>POPName</attCode><attValue>Smart Village Cabinet 952</attValue></addList><cityCode>2</cityCode><complainNo>23650030402</complainNo><cabinetNo>SV CABINET 502</cabinetNo><dpLongitude/><dpLatitude/><exchCode>MBKGZ</exchCode><initiator>FCC</initiator><notes>تعديل رقم المتابعة من  بواسطة </notes><priCode>Normal</priCode><detractorFlag>0</detractorFlag><requestType>FTTHWrongCardPort</requestType><telNo>11111112</telNo></WorkOrderInfo></fcc:CreateWo></SOAP-ENV:Body></SOAP-ENV:Envelope>";
    // Pause between sending one work order and the next
    private static final long DELAY_BETWEEN_ORDERS_MS = 100;
    // QUOTA and NORM_QUOTA are set to this for the whole run, then put back to the original amounts at the end
    private static final int QUOTA_DURING_RUN = 2000;
    Export export = new Export();
    private String IP;
    private Database database = new Database();
    private String Port;
    private String URL_FTTH_Maintenance;
    private String URL_installation;
    private String URL_DATA_Maintenance;
    private ArrayList<String> all_WOs = new ArrayList<String>();
    // FTTHNewSubVS orders are never scheduled, so they are waited on separately (until Partial Assign)
    private ArrayList<String> new_sub_WOs = new ArrayList<String>();
    // Every work order written to My_tasks_work_orders.csv in this run, with the user ID of the user on its row
    private LinkedHashMap<String, String> my_tasks_WOs = new LinkedHashMap<String, String>();

    // One template per work order type, grouped by the endpoint they are sent to
    private ArrayList<XML> xmls_installation = new ArrayList<XML>();
    private ArrayList<XML> xmls_DATA_Maintenance = new ArrayList<XML>();
    private ArrayList<XML> xmls_FTTH_Maintenance = new ArrayList<XML>();

    /**
     * Builds the endpoint URLs for the given server and parses all request templates.
     * Nothing is sent to the server here.
     */
    public Create(String IP, String Port) throws XPathExpressionException, ParserConfigurationException, IOException, TransformerException, SAXException, SQLException {
        this.IP=IP;
        this.Port=Port;
        String base_url = "http://"+this.IP+":"+this.Port;
        this.URL_FTTH_Maintenance = base_url+"/FCCWFMInteg-FCCWFMInteg-context-root/FCCWFMIntegPort";
        this.URL_installation = base_url+"/HiveAPIs/resources/hivews/CreateOrder";
        this.URL_DATA_Maintenance = base_url+"/FCCHiveWS/WSPort";

        //xmls_installation.add(new XML(this.AVReSplitReserve_body,"AVReSplitReserve"));
        xmls_installation.add(new XML(this.ADSL_Change_to_VDSL,"ADSL Change to VDSL"));
        xmls_installation.add(new XML(this.FTTHNewSubVS,"FTTHNewSubVS"));
        //xmls_installation.add(new XML(this.FTTHMigrationSurvey,"FTTHMigrationSurvey"));
        xmls_installation.add(new XML(this.FTTHUnRchReSurvey,"FTTHUnRchReSurvey"));
        xmls_installation.add(new XML(this.ADSL_VDSL_Splitting,"ADSL/VDSL Splitting"));
        xmls_DATA_Maintenance.add(new XML(this.Data_Down,"Data_Down"));
        xmls_DATA_Maintenance.add(new XML(this.Physical_Instability,"Physical_Instability"));

        //xmls_FTTH_Maintenance.add(new XML(this.FTTHDataVoiceDown,"FTTHDataVoiceDown"));
        //xmls_FTTH_Maintenance.add(new XML(this.FTTHWrongCardPort,"FTTHWrongCardPort"));
    }

    // Share of each CSV file, in the order of the indexes below.
    // Based on a 2280-order run: 431 each for Closed, Opened, Zone Tasks and My Tasks, 278 each for Assign and Re-assign.
    static final int[] FILE_WEIGHTS = {431, 431, 431, 431, 278, 278};
    static final int CLOSED = 0, OPENED = 1, ZONE_TASKS = 2, MY_TASKS = 3, ASSIGN = 4, RE_ASSIGN = 5;

    // How many orders each CSV file gets in this run (indexed like FILE_WEIGHTS). Set by genrate_work_orders.
    private int[] file_sizes;

    /**
     * Creates the requested number of work orders and returns the ones that export_work_orders
     * still has to place (everything except FTTHNewSubVS and DATA maintenance).
     * <p>
     * First the total is split over the CSV files by FILE_WEIGHTS, then the order types are sized to fit:
     * Closed           -> not created: already closed orders are read from the database, written here
     * FTTHNewSubVS     -> exactly My Tasks + Assign (and only those two files), written here, sent right after the first order
     * DATA maintenance -> exactly Zone Tasks (Data Down and Physical Instability), written here
     * all other types  -> Opened + Re-assign, shared evenly between the types
     */
    public ArrayList<String> genrate_work_orders ( int number_of_work_orders) throws Exception {
        ArrayList <String> work_orders = new ArrayList<String>();
        file_sizes = split_by_weights(number_of_work_orders, FILE_WEIGHTS);
        int closed_count = file_sizes[CLOSED];
        int new_sub_count = file_sizes[MY_TASKS] + file_sizes[ASSIGN];
        int data_count = file_sizes[ZONE_TASKS];

        // Done first, so a wrong or empty closed SELECT stops the run before any order is sent
        for (String work_order_id : select_closed_work_orders(closed_count)) {
            export.Closed_Page_work_orders(work_order_id);
        }

        // Fixed quota for the run (more only if there are more FTTHNewSubVS orders than that); restored at the end
        int quota = Math.max(QUOTA_DURING_RUN, new_sub_count);
        System.out.println("Raising zone quota to " + quota + "...");
        database.raise_quota(quota);
        //System.out.println("Setting scheduling type to MANUAL...");
        database.update();
        System.out.println("Database ready, sending orders...");

        int other_count = number_of_work_orders - closed_count - new_sub_count - data_count;

        // Installation templates other than FTTHNewSubVS share the "other" orders with FTTH maintenance
        XML new_sub = null;
        ArrayList<XML> other_installation = new ArrayList<XML>();
        for (XML xml : xmls_installation) {
            if (xml.get_request_type().equals("FTTHNewSubVS")) new_sub = xml;
            else other_installation.add(xml);
        }
        int[] dataPerType = distribute_orders(data_count, xmls_DATA_Maintenance.size());
        int[] otherPerType = distribute_orders(other_count, xmls_FTTH_Maintenance.size() + other_installation.size());

        // Position in otherPerType. It keeps counting across FTTH maintenance and installation
        int type_index = 0;
        orders_sent = 0;
        pending_new_sub = new_sub;
        pending_new_sub_count = new_sub_count;

        for (XML xml : xmls_FTTH_Maintenance)
        {
            for (int j = 0; j < otherPerType[type_index]; j++)
            {
                String work_order_id = send(xml, this.URL_FTTH_Maintenance, true);
                work_orders.add(work_order_id);
                this.all_WOs.add(work_order_id);
            }
            type_index++;
        }
        for (int t = 0; t < xmls_DATA_Maintenance.size(); t++)
        {
            for (int j = 0; j < dataPerType[t]; j++)
            {
                String work_order_id = send(xmls_DATA_Maintenance.get(t), this.URL_DATA_Maintenance, true);
                export.Zone_Tasks_work_orders(work_order_id);
                this.all_WOs.add(work_order_id);
            }
        }
        for (XML xml : other_installation)
        {
            for (int j = 0; j < otherPerType[type_index]; j++)
            {
                String work_order_id = send(xml, this.URL_installation, false);
                work_orders.add(work_order_id);
                this.all_WOs.add(work_order_id);
            }
            type_index++;
        }
        // Only needed when no other order was sent, otherwise they already went out after the first order
        send_new_sub_orders();
        return work_orders;
    }

    /**
     * Sends the pending FTTHNewSubVS orders (set in genrate_work_orders), once per run.
     * The first ones fill Assign, the rest go to My Tasks.
     */
    private void send_new_sub_orders() throws Exception {
        if (pending_new_sub == null) return;
        XML new_sub = pending_new_sub;
        pending_new_sub = null;   // cleared first, because send() below would otherwise call this again
        for (int j = 0; j < pending_new_sub_count; j++)
        {
            String work_order_id = send(new_sub, this.URL_installation, false);
            this.new_sub_WOs.add(work_order_id);
            if (j < file_sizes[ASSIGN]) {
                export.Assign_work_orders(work_order_id);
            } else {
                String user_id = export.My_tasks_work_orders(work_order_id);
                this.my_tasks_WOs.put(work_order_id, user_id);
            }
        }
    }

    // How many orders were sent so far in this run, so the pause is only between orders, not before the first
    private int orders_sent;

    // FTTHNewSubVS template and count still to send; they are sent right after the first order of the run
    private XML pending_new_sub;
    private int pending_new_sub_count;

    /**
     * Sends one order from the template, pausing first unless it is the first order of the run.
     * Right after the first order, all FTTHNewSubVS orders are sent before anything else.
     */
    private String send(XML xml, String url, boolean maintenance) throws Exception {
        if (orders_sent++ > 0) Thread.sleep(DELAY_BETWEEN_ORDERS_MS);
        String work_order_id = maintenance ? send_maintenance(xml, url) : send_installation(xml, url);
        if (orders_sent == 1) send_new_sub_orders();
        return work_order_id;
    }

    /**
     * Reads count already closed work orders from the database for Closed_Page_work_orders.csv.
     * Tests override this to avoid calling a real database.
     */
    protected ArrayList<String> select_closed_work_orders(int count) throws SQLException {
        return database.select_closed_work_orders(count);
    }

    /**
     * Gives the template a new complainNo and sends it as a maintenance order.
     * Tests override this to avoid calling a real server.
     */
    protected String send_maintenance(XML xml, String url) throws Exception {
        return xml.send_creation_request_Maintenance(xml.update_complain_number(), url);
    }

    /**
     * Gives the template a new OMOrderID and sends it as an installation order.
     * Tests override this to avoid calling a real server.
     */
    protected String send_installation(XML xml, String url) throws Exception {
        return xml.send_creation_request_installation(xml.randomize_OM_Order_ID(), url);
    }

    /**
     * Shuffles the work orders returned by genrate_work_orders and splits them at random across
     * Opened Page and Re-assign, with the sizes worked out in genrate_work_orders.
     * Closed Page was already filled from the database in genrate_work_orders.
     * The list passed in is not changed.
     */
    public void export_work_orders(ArrayList<String> work_orders) throws IOException, SQLException, InterruptedException {
        ArrayList<String> shuffled = new ArrayList<String>(work_orders);
        Collections.shuffle(shuffled);

        // When the list is exactly the one genrate_work_orders returned, this gives exactly these sizes
        int[] partSizes = split_by_weights(shuffled.size(),
                new int[]{file_sizes[OPENED], file_sizes[RE_ASSIGN]});

        // Use the same Export as genrate_work_orders, so orders already written this run are kept
        int i = 0;
        for (int part = 0; part < partSizes.length; part++) {
            for (int k = 0; k < partSizes[part]; k++, i++) {
                String order = shuffled.get(i);
                switch (part) {
                    case 0 -> export.Opened_Page_work_orders(order);
                    default -> export.Re_assigen_work_orders(order);
                }
            }
        }
        System.out.println("Orders per file: Closed " + file_sizes[CLOSED] + ", Opened " + file_sizes[OPENED]
                + ", Zone Tasks " + file_sizes[ZONE_TASKS] + ", My Tasks " + file_sizes[MY_TASKS]
                + ", Assign " + file_sizes[ASSIGN] + ", Re-assign " + file_sizes[RE_ASSIGN]);
        database.wait_until_scheduled(this.all_WOs);
        // Keeps the quota raised until every FTTHNewSubVS order has taken its quota
        database.wait_until_assigned(this.new_sub_WOs);
    }

    /**
     * Splits n orders into parts in proportion to the given weights.
     * Each part first gets its rounded-down share. The leftover orders then go one by one to the
     * parts that lost the most in the rounding; on a tie the earlier part wins.
     * Example: 2280 orders with FILE_WEIGHTS gives [431, 431, 431, 431, 278, 278].
     */
    static int[] split_by_weights(int n, int[] weights) {
        long total = 0;
        for (int w : weights) total += w;
        int[] sizes = new int[weights.length];
        if (total == 0) return sizes;
        int leftover = n;
        for (int p = 0; p < weights.length; p++) {
            sizes[p] = (int) ((long) n * weights[p] / total);
            leftover -= sizes[p];
        }
        boolean[] got_extra = new boolean[weights.length];
        for (; leftover > 0; leftover--) {
            int best = -1;
            for (int p = 0; p < weights.length; p++) {
                if (!got_extra[p] && (best == -1 || (long) n * weights[p] % total > (long) n * weights[best] % total)) {
                    best = p;
                }
            }
            sizes[best]++;
            got_extra[best] = true;
        }
        return sizes;
    }

    /**
     * Calls the My Tasks database function once for every work order in My_tasks_work_orders.csv,
     * assigning it to the user ID of the user on its row.
     * Call after export_work_orders, which waits until the orders are Partial Assign.
     */
    public void call_my_tasks_function() throws SQLException {
        database.call_my_tasks_function(this.my_tasks_WOs);
    }

    /** Puts the zone quota back to its original amount. Safe to call even if it was never raised. */
    public void restore_quota() throws SQLException {
        System.out.println("Restoring the original zone quota...");
        database.rollback_quota();
    }

    /** Closes the database connection. Call once at the end of the run. */
    public void close_database() {
        database.close();
    }

    /**
     * Splits number_of_orders over number_of_types as evenly as possible.
     * Example: 10 orders over 8 types gives [2, 2, 1, 1, 1, 1, 1, 1].
     */
    public int[] distribute_orders(int number_of_orders, int number_of_types) {
        int[] ordersPerType = new int[number_of_types];

        int baseOrders = number_of_orders / number_of_types;
        int remainder = number_of_orders % number_of_types;

        for (int i = 0; i < number_of_types; i++) {
            ordersPerType[i] = baseOrders + (i < remainder ? 1 : 0);
        }

        return ordersPerType;
    }

    public String get_URL_FTTH_Maintenance() { return URL_FTTH_Maintenance; }
    public String get_URL_installation() { return URL_installation; }
    public String get_URL_DATA_Maintenance() { return URL_DATA_Maintenance; }


}
