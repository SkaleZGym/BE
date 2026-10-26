package com.skalez.shipping.domain;

public enum Courier {
    CJ("CJ대한통운", "https://trace.cjlogistics.com/next/tracking.html?wblNo={no}"),
    LOTTE("롯데택배", "https://www.lotteglogis.com/home/reservation/tracking/linkView?InvNo={no}"),
    HANJIN("한진택배", "https://www.hanjin.com/kor/CMS/DeliveryMgr/WaybillResult.do?mCode=MN038&schLang=KR&wblnumText2={no}"),
    EPOST("우체국택배", "https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm?sid1={no}"),
    LOGEN("로젠택배", "https://www.ilogen.com/web/personal/trace/{no}");

    private final String displayName;
    private final String trackingUrlTemplate;
    Courier(String displayName, String trackingUrlTemplate){
        this.displayName = displayName;
        this.trackingUrlTemplate = trackingUrlTemplate;
    }
    public String displayName(){return displayName;}
    public String trackingUrl(String trackingNumber){return trackingUrlTemplate.replace("{no}", trackingNumber);}
}
