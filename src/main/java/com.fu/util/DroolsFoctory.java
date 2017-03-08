package com.fu.util;

import java.util.Map;

import org.kie.api.KieServices;
import org.kie.api.builder.KieScanner;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import com.taobao.diamond.manager.impl.WholeDiamondManager;
import com.umetrip.insure.claimrules.domain.ClaimStatus;
import com.umetrip.insure.claimrules.domain.FlightStatus;
import com.umetrip.insure.claimrules.domain.ProductParam;
import com.umetrip.insure.premium.calculate.domain.UmePremium;


public class DroolsUtil {

    private static class SingletonHolder {
        private static final DroolsUtil INSTANCE = new DroolsUtil();
    }

    private DroolsUtil(){}

    public static final DroolsUtil getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static KieContainer kContainerInternal =null;

    private static KieContainer kContainerRelease =null;

    private static KieScanner kScannerInsure =null;

    //加载工程内部规则jar包
    private synchronized KieContainer getKContainerInternal(){
        if (kContainerInternal==null){
            LogUtil.info(DroolsUtil.class,"DroolsUtil info: init KieContainer...");
            KieServices ks = KieServices.Factory.get();
            kContainerInternal = ks.getKieClasspathContainer();
        }else{
            LogUtil.info(DroolsUtil.class,"DroolsUtil info: use old KieContainer...");
        }
        return kContainerInternal;
    }

    //动态加载外部规则jar包
    private synchronized KieContainer getInsureKieContainerRelease(){
        if (kContainerRelease==null){
            KieServices kieServices = KieServices.Factory.get();
            WholeDiamondManager diamondManager = WholeDiamondManager.getInstance();
            String version = diamondManager.getConfigureInfomation("InsureDrools_Version", "UmeDrools", 50000);
            ReleaseId releaseId = kieServices.newReleaseId( "com.umetrip.insure",
                    "InsureDrools", version);
//			ReleaseId releaseId = kieServices.newReleaseId( "com.umetrip.insure",
//					"InsureDrools", "RELEASE" );
            kContainerRelease = kieServices.newKieContainer( releaseId );
            kScannerInsure = kieServices.newKieScanner( kContainerRelease );
            LogUtil.info(DroolsUtil.class,"DroolsUtil info: begin kScanner..........................");
            kScannerInsure.start( 30000L );
        }else{
            LogUtil.info(DroolsUtil.class,"DroolsUtil info: use old KieContainer..........................");
        }
        return kContainerRelease;
    }

    public void umePremiumFire(UmePremium umePremium){
        KieContainer kiecontainer = getInsureKieContainerRelease();
        if (kiecontainer!=null){
            KieSession kSession = kiecontainer.newKieSession("ksession-umepremiumrules");
            kSession.insert(umePremium);
            kSession.fireAllRules();
        }
        //kSession.dispose();
    }

    public void singlePayclaimRulesFire(ClaimStatus claimStatus, FlightStatus flightStatus, ProductParam productParam){
        KieContainer kiecontainer = getInsureKieContainerRelease();
        //KieContainer kiecontainer = getKContainerInternal();
        if (kiecontainer!=null){
            KieSession kSession = kiecontainer.newKieSession("ksession-singlepayclaimrules");
            kSession.insert(claimStatus);
            kSession.insert(flightStatus);
            kSession.insert(productParam);
            kSession.fireAllRules();
        }
        //kSession.dispose();
    }

    public void multiPayclaimRulesFire(ClaimStatus claimStatus, FlightStatus flightStatus, ProductParam productParam){
        KieContainer kiecontainer = getInsureKieContainerRelease();
        //KieContainer kiecontainer = getKContainerInternal();
        if (kiecontainer!=null){
            KieSession kSession = kiecontainer.newKieSession("ksession-multipayclaimrules");
            kSession.insert(claimStatus);
            kSession.insert(flightStatus);
            kSession.insert(productParam);
            kSession.fireAllRules();
        }
        //kSession.dispose();
    }

    public synchronized Map<String,Object> setkScannerInsure(String call ,Long interval){
        if (kScannerInsure ==null){
            LogUtil.error(DroolsUtil.class, "DroolsUtil error: kScannerInsure is null!");
            return ReturnMapUtil.getFailMap("DroolsUtil error: kScannerInsure is null!");
        }else {
            try {
                if ("stop".equals(call)){
                    kScannerInsure.stop();
                    LogUtil.info(DroolsUtil.class, "DroolsUtil stop kScannerInsure info: stop kScannerInsure success....");
                }else if ("start".equals(call)){
                    kScannerInsure.start(interval);
                    LogUtil.info(DroolsUtil.class, "DroolsUtil start kScannerInsure info: start kScannerInsure....");
                }else if ("shutdown".equals(call)){
                    kScannerInsure.shutdown();
                    LogUtil.info(DroolsUtil.class, "DroolsUtil shutdown kScannerInsure info: shutdown kScannerInsure....");
                }else if ("scan".equals(call)){
                    kScannerInsure.scanNow();
                    LogUtil.info(DroolsUtil.class, "DroolsUtil scan kScannerInsure info: scan kScannerInsure....");
                }else if ("restart".equals(call)){
                    LogUtil.info(DroolsUtil.class, "DroolsUtil restart kScannerInsure restart : shutdown kScannerInsure first....");
                    kScannerInsure.shutdown();
                    LogUtil.info(DroolsUtil.class, "DroolsUtil restart kScannerInsure info: shutdown kScannerInsure success....");
                    KieServices kieServices = KieServices.Factory.get();
                    kScannerInsure = kieServices.newKieScanner( kContainerRelease );
                    kScannerInsure.start( interval );
                    LogUtil.info(DroolsUtil.class, "DroolsUtil restart kScannerInsure info: start new kScannerInsure success....");
                }else {
                    LogUtil.info(DroolsUtil.class, "DroolsUtil info: unsupport kScannerInsure....");
                }
            } catch (Exception e) {
                LogUtil.error(DroolsUtil.class, "DroolsUtil exception: "+e.toString());
                return ReturnMapUtil.getFailMap("DroolsUtil exception: "+e.toString());
            }
        }
        return ReturnMapUtil.getSuccessMap("DroolsUtil info: "+call +" success!");
    }
}
