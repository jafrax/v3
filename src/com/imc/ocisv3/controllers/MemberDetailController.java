package com.imc.ocisv3.controllers;

import com.imc.ocisv3.pojos.BenefitPOJO;
import com.imc.ocisv3.pojos.ClaimPOJO;
import com.imc.ocisv3.pojos.MemberPOJO;
import com.imc.ocisv3.pojos.PolicyPOJO;
import com.imc.ocisv3.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by faizal on 10/24/13.
 */
public class MemberDetailController extends Window {

    private Logger log = LoggerFactory.getLogger(MemberDetailController.class);
    private PolicyPOJO policy;
    private MemberPOJO member;
    private Listbox lbRelatives;
    private Listbox lbClaimHistory;
    private Listbox lbPolicyClaimHistory;
    private Listbox lbPlan;
    private Listbox lbPlanItems;
    private Listbox lbUpdation;
    private Paging pgClaimHistory;
    private Tab tabRelatives;
    private Tab tabClaimHistory;
    private Tab tabPolicyClaimHistory;
    private Listfooter ftrApproved;
    private Listfooter ftrPolicyApproved;

    private double familyLimit = 0;
    private String familyLimitPlan = "";
    private double memberUsage = 0;
    private double policyUsage = 0;
    private Map<String,String> clientPlanMap;

    public void onCreate() {
        policy = (PolicyPOJO) getAttribute("policy");
        member = (MemberPOJO) getAttribute("member");
        initComponents();
        populate();
    }

    private void initComponents() {
        lbRelatives = (Listbox) getFellow("lbRelatives");
        lbClaimHistory = (Listbox) getFellow("lbClaimHistory");
        lbPolicyClaimHistory = (Listbox) getFellow("lbPolicyClaimHistory");
        lbPlan = (Listbox) getFellow("lbPlan");
        lbPlanItems = (Listbox) getFellow("lbPlanItems");
        lbUpdation = (Listbox) getFellow("lbUpdation");
        pgClaimHistory = (Paging) getFellow("pgClaimHistory");
        tabRelatives = (Tab) getFellow("tabRelatives");
        tabClaimHistory = (Tab) getFellow("tabClaimHistory");
        tabPolicyClaimHistory = (Tab) getFellow("tabPolicyClaimHistory");
        ftrApproved = (Listfooter) getFellow("ftrApproved");
        ftrPolicyApproved = (Listfooter) getFellow("ftrPolicyApproved");
    }

    private void populate() {
        clientPlanMap = Libs.getClientPlanMap(policy.getPolicy_string());

        tabRelatives.setDisabled(true);
        tabClaimHistory.setDisabled(true);
        getCaption().setLabel("Member Detail [" + member.getCard_number() + " - " + member.getName() + "]");
        populateInformation();
        populateClaimHistory(0, pgClaimHistory.getPageSize());
        populatePlanRegistered();
        populateRelatives();
        populateUpdation();
    }

    private void populateInformation() {
        int ageDays = 0;
        try {
            ageDays = Libs.getDiffDays(new SimpleDateFormat("yyyy-MM-dd").parse(member.getDob()), new Date());
        } catch (Exception ex) {
            log.error("populateInformation", ex);
        }

        ((Label) getFellow("lName")).setValue(member.getName());
        ((Label) getFellow("lDOB")).setValue(member.getDob());

        Label lAge = (Label) getFellow("lAge");
        lAge.setStyle("text-align:right;");
        lAge.setValue("(" + (ageDays / 365) + ")");

        ((Label) getFellow("lSex")).setValue(member.getSex());
        ((Label) getFellow("lStartingDate")).setValue(member.getStarting_date());
        ((Label) getFellow("lMatureDate")).setValue(member.getMature_date());
        ((Label) getFellow("lClientPolicyNumber")).setValue(member.getClient_policy_number());
        ((Label) getFellow("lClientIDNumber")).setValue(member.getClient_id_number());
        ((Label) getFellow("lCardNumber")).setValue(member.getCard_number());
        ((Label) getFellow("lMaritalStatus")).setValue(member.getMarital_status());
    }

    private void populateRelatives() {
        if (familyLimit>0) {
            Double[] remainingFamilyLimit = Libs.getRemainingFamilyLimit(policy.getYear() + "-" + policy.getBr() + "-" + policy.getDist() + "-" + policy.getPolicy_number(), member.getIdx() + "-" + member.getSeq(), familyLimitPlan);
            double edcUsage = Libs.getEDCUsage(policy.getYear() + "-" + policy.getBr() + "-" + policy.getDist() + "-" + policy.getPolicy_number(), member.getIdx());
            ((East) getFellow("eastFamilyLimit")).setOpen(true);
            ((Label) getFellow("lFamilyLimit")).setValue(new DecimalFormat("#,###.##").format(familyLimit));
            ((Label) getFellow("lMemberUsage")).setValue(new DecimalFormat("#,###.##").format(memberUsage));
            ((Label) getFellow("lRemainingLimit")).setValue(new DecimalFormat("#,###.##").format(remainingFamilyLimit[0]-edcUsage));
            ((Label) getFellow("lFamilyUsage")).setValue(new DecimalFormat("#,###.##").format(remainingFamilyLimit[1]));
            ((Label) getFellow("lEDCUsage")).setValue(new DecimalFormat("#,###.##").format(edcUsage));
        } else {
            ((East) getFellow("eastFamilyLimit")).setOpen(false);
        }

        lbRelatives.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "a.hdt1ncard, a.hdt1name, "
                    + "a.hdt1bdtyy, a.hdt1bdtmm, a.hdt1bdtdd, "
                    + "a.hdt1sex, "
                    + "b.hdt2plan1, b.hdt2plan2, b.hdt2plan3, b.hdt2plan4, b.hdt2plan5, b.hdt2plan6, "
                    + "b.hdt2sdtyy, b.hdt2sdtmm, b.hdt2sdtdd, "
                    + "b.hdt2mdtyy, b.hdt2mdtmm, b.hdt2mdtdd, "
                    + "a.hdt1idxno, a.hdt1seqno, "
                    + "c.hempcnid, " // 20
                    + "(convert(varchar,b.hdt2pedty1)+'-'+convert(varchar,b.hdt2pedtm1)+'-'+convert(varchar,b.hdt2pedtd1)) as hdt2pedt1,"
                    + "(convert(varchar,b.hdt2pedty2)+'-'+convert(varchar,b.hdt2pedtm2)+'-'+convert(varchar,b.hdt2pedtd2)) as hdt2pedt2,"
                    + "(convert(varchar,b.hdt2pedty3)+'-'+convert(varchar,b.hdt2pedtm3)+'-'+convert(varchar,b.hdt2pedtd3)) as hdt2pedt3,"
                    + "(convert(varchar,b.hdt2pedty4)+'-'+convert(varchar,b.hdt2pedtm4)+'-'+convert(varchar,b.hdt2pedtd4)) as hdt2pedt4,"
                    + "(convert(varchar,b.hdt2pedty5)+'-'+convert(varchar,b.hdt2pedtm5)+'-'+convert(varchar,b.hdt2pedtd5)) as hdt2pedt5,"
                    + "(convert(varchar,b.hdt2pedty6)+'-'+convert(varchar,b.hdt2pedtm6)+'-'+convert(varchar,b.hdt2pedtd6)) as hdt2pedt6,"
                    + "(convert(varchar,b.hdt2pxdty1)+'-'+convert(varchar,b.hdt2pxdtm1)+'-'+convert(varchar,b.hdt2pxdtd1)) as hdt2pxdt1,"
                    + "(convert(varchar,b.hdt2pxdty2)+'-'+convert(varchar,b.hdt2pxdtm2)+'-'+convert(varchar,b.hdt2pxdtd2)) as hdt2pxdt2,"
                    + "(convert(varchar,b.hdt2pxdty3)+'-'+convert(varchar,b.hdt2pxdtm3)+'-'+convert(varchar,b.hdt2pxdtd3)) as hdt2pxdt3,"
                    + "(convert(varchar,b.hdt2pxdty4)+'-'+convert(varchar,b.hdt2pxdtm4)+'-'+convert(varchar,b.hdt2pxdtd4)) as hdt2pxdt4,"
                    + "(convert(varchar,b.hdt2pxdty5)+'-'+convert(varchar,b.hdt2pxdtm5)+'-'+convert(varchar,b.hdt2pxdtd5)) as hdt2pxdt5,"
                    + "(convert(varchar,b.hdt2pxdty6)+'-'+convert(varchar,b.hdt2pxdtm6)+'-'+convert(varchar,b.hdt2pxdtd6)) as hdt2pxdt6, "
                    + "a.hdt1mstat, c.hempcnpol, "
                    + "b.hdt2moe "
                    + "from idnhltpf.dbo.hltdt1 a "
                    + "inner join idnhltpf.dbo.hltdt2 b on b.hdt2yy=a.hdt1yy and b.hdt2pono=a.hdt1pono and b.hdt2idxno=a.hdt1idxno and b.hdt2seqno=a.hdt1seqno and b.hdt2ctr=a.hdt1ctr "
                    + "inner join idnhltpf.dbo.hltemp c on c.hempyy=a.hdt1yy and c.hemppono=a.hdt1pono and c.hempidxno=a.hdt1idxno and c.hempseqno=a.hdt1seqno and c.hempctr=a.hdt1ctr "
                    + "where "
                    + "a.hdt1yy=" + policy.getYear() + " and a.hdt1pono=" + policy.getPolicy_number() + " "
                    + "and a.hdt1idxno=" + member.getIdx() + " and a.hdt1seqno<>'" + member.getSeq() + "' "
                    + "and a.hdt1ctr=0 "
                    + "and a.hdt1idxno<>99999 "
                    + "order by a.hdt1seqno asc ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                MemberPOJO memberPOJO = new MemberPOJO();
                memberPOJO.setPolicy(policy);
                memberPOJO.setName(Libs.nn(o[1]).trim());
                memberPOJO.setCard_number(Libs.nn(o[0]).trim());
                memberPOJO.setDob(Libs.nn(o[2]) + "-" + Libs.nn(o[3]) + "-" + Libs.nn(o[4]));
                memberPOJO.setStarting_date(Libs.nn(o[12]) + "-" + Libs.nn(o[13]) + "-" + Libs.nn(o[14]));
                memberPOJO.setMature_date(Libs.nn(o[15]) + "-" + Libs.nn(o[16]) + "-" + Libs.nn(o[17]));
                memberPOJO.setSex(Libs.nn(o[5]));
                memberPOJO.setIp(Libs.nn(o[6]).trim());
                memberPOJO.setOp(Libs.nn(o[7]).trim());
                memberPOJO.setMaternity(Libs.nn(o[8]).trim());
                memberPOJO.setDental(Libs.nn(o[9]).trim());
                memberPOJO.setGlasses(Libs.nn(o[10]).trim());
                memberPOJO.setMarital_status(Libs.nn(o[33]));
                memberPOJO.setIdx(Libs.nn(o[18]));
                memberPOJO.setSeq(Libs.nn(o[19]));
                memberPOJO.setClient_policy_number(Libs.nn(o[34]).trim());
                memberPOJO.setClient_id_number(Libs.nn(o[20]).trim());

                memberPOJO.getPlan_entry_date().add(Libs.nn(o[21]));
                memberPOJO.getPlan_entry_date().add(Libs.nn(o[22]));
                memberPOJO.getPlan_entry_date().add(Libs.nn(o[23]));
                memberPOJO.getPlan_entry_date().add(Libs.nn(o[24]));
                memberPOJO.getPlan_entry_date().add(Libs.nn(o[25]));
                memberPOJO.getPlan_entry_date().add(Libs.nn(o[26]));
                memberPOJO.getPlan_exit_date().add(Libs.nn(o[27]));
                memberPOJO.getPlan_exit_date().add(Libs.nn(o[28]));
                memberPOJO.getPlan_exit_date().add(Libs.nn(o[29]));
                memberPOJO.getPlan_exit_date().add(Libs.nn(o[30]));
                memberPOJO.getPlan_exit_date().add(Libs.nn(o[31]));
                memberPOJO.getPlan_exit_date().add(Libs.nn(o[32]));

                int ageDays = Libs.getDiffDays(new SimpleDateFormat("yyyy-MM-dd").parse(memberPOJO.getDob()), new Date());
                int matureDays = Libs.getDiffDays(new Date(), new SimpleDateFormat("yyyy-MM-dd").parse(memberPOJO.getMature_date()));

                Listcell lcStatus = new Listcell();
                Label lStatus = new Label();
                if (matureDays>0) {
                    lStatus.setValue("ACTIVE");
                    lStatus.setStyle("color:#00FF00");
                } else {
                    lStatus.setValue("MATURE");
                    lStatus.setStyle("color:#FF0000;");
                }
                if (Libs.nn(o[35]).equals("U")) {
                    String effectiveDate = Libs.nn(o[41]) + "-" + Libs.nn(o[42]) + "-" + Libs.nn(o[43]);
                    int effectiveDays = Libs.getDiffDays(new Date(), new SimpleDateFormat("yyyy-MM-dd").parse(effectiveDate));
                    if (effectiveDays<0) {
                        lStatus.setValue("INACTIVE");
                        lStatus.setStyle("color:#000000;");
                    }
                }

                lcStatus.appendChild(lStatus);

                Listitem li = new Listitem();
                li.setValue(memberPOJO);

                li.appendChild(new Listcell(memberPOJO.getCard_number()));
                li.appendChild(lcStatus);
                li.appendChild(new Listcell(memberPOJO.getName()));
                li.appendChild(new Listcell(memberPOJO.getDob()));
                li.appendChild(Libs.createNumericListcell(ageDays/365, "#"));
                li.appendChild(new Listcell(memberPOJO.getSex()));
                li.appendChild(new Listcell((clientPlanMap.get(memberPOJO.getIp())==null ? memberPOJO.getIp() : clientPlanMap.get(memberPOJO.getIp()))));
                li.appendChild(new Listcell((clientPlanMap.get(memberPOJO.getOp())==null ? memberPOJO.getOp() : clientPlanMap.get(memberPOJO.getOp()))));
                li.appendChild(new Listcell((clientPlanMap.get(memberPOJO.getMaternity())==null ? memberPOJO.getMaternity() : clientPlanMap.get(memberPOJO.getMaternity()))));
                li.appendChild(new Listcell((clientPlanMap.get(memberPOJO.getDental())==null ? memberPOJO.getDental() : clientPlanMap.get(memberPOJO.getDental()))));
                li.appendChild(new Listcell((clientPlanMap.get(memberPOJO.getGlasses())==null ? memberPOJO.getGlasses() : clientPlanMap.get(memberPOJO.getGlasses()))));
                li.appendChild(new Listcell(memberPOJO.getStarting_date()));
                li.appendChild(new Listcell(memberPOJO.getMature_date()));
                lbRelatives.appendChild(li);
            }

            if (l.size()>0) tabRelatives.setDisabled(false);
        } catch (Exception ex) {
            log.error("populateRelatives", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }

    }

    private void populateClaimHistory(int offset, int limit) {
        lbClaimHistory.getItems().clear();
        lbPolicyClaimHistory.getItems().clear();

        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "a.hclmtclaim, b.hproname, "
                    + "a.hclmdiscd1, a.hclmdiscd2, a.hclmdiscd3, "
                    + "a.hclmsinyy, a.hclmsinmm, a.hclmsindd, "
                    + "a.hclmsoutyy, a.hclmsoutmm, a.hclmsoutdd, "
                    + "(a.hclmpcode1 + a.hclmpcode2) as plan_code, "
                    + "a.hclmcno, "
                    + "a.hclmcount, "
                    + "(" + Libs.createAddFieldString("a.hclmcamt") + ") as proposed, "
                    + "(" + Libs.createAddFieldString("a.hclmaamt") + ") as approved, "
                    + "a.hclmseqno, " //16
                    + "c.hdt1name, "
                    + "a.hclmidxno, "
                    + "d.hmem2data1 "
                    + "from idnhltpf.dbo.hltclm a "
                    + "inner join idnhltpf.dbo.hltpro b on b.hpronomor=a.hclmnhoscd "
                    + "inner join idnhltpf.dbo.hltdt1 c on c.hdt1yy=a.hclmyy and c.hdt1pono=a.hclmpono and c.hdt1idxno=a.hclmidxno and c.hdt1seqno=a.hclmseqno and c.hdt1ctr=0 "
                    + "left outer join idnhltpf.dbo.hltmemo2 d on d.hmem2yy=a.hclmyy and d.hmem2pono=a.hclmpono and d.hmem2idxno=a.hclmidxno and d.hmem2seqno=a.hclmseqno and d.hmem2claim=a.hclmtclaim and d.hmem2count=a.hclmcount "
                    + "where "
                    + "a.hclmyy=" + policy.getYear() + " and a.hclmpono=" + policy.getPolicy_number() + " "
                    + "and a.hclmidxno=" + member.getIdx() + " "
                    + "and a.hclmrecid<>'C' "
                    + "order by a.hclmcdatey desc, a.hclmcdatem desc, a.hclmcdated desc ";

            int memberClaimCount = 0;
            int policyClaimCount = 0;

            List<Object[]> l = s.createSQLQuery(qry).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                policyClaimCount++;

                String diagnosis = Libs.nn(o[2]).trim();
                if (!Libs.nn(o[3]).trim().isEmpty()) diagnosis += ", " + Libs.nn(o[3]).trim();
                if (!Libs.nn(o[4]).trim().isEmpty()) diagnosis += ", " + Libs.nn(o[4]).trim();

                String serviceIn = Libs.nn(o[5]) + "-" + Libs.nn(o[6]) + "-" + Libs.nn(o[7]);
                String serviceOut = Libs.nn(o[8]) + "-" + Libs.nn(o[9]) + "-" + Libs.nn(o[10]);

                String claimType = Libs.nn(o[0]);
                if (claimType.equals("I")) claimType = "INPATIENT";
                if (claimType.equals("O")) claimType = "OUTPATIENT";
                if (claimType.equals("G")) claimType = "GLASSES";
                if (claimType.equals("R")) claimType = "MATERNITY";
                if (claimType.equals("D")) claimType = "DENTAL";

                String planString = clientPlanMap.get(Libs.nn(o[11]));
                if (planString==null) planString = Libs.nn(o[11]);

//                Claim History
                if (Libs.nn(o[16]).equals(Libs.nn(member.getSeq()))) {
                    memberClaimCount++;
                    memberUsage += Double.valueOf(Libs.nn(o[15]));

                    String remarks = Libs.nn(o[19]).trim();
                    String provider = Libs.nn(o[1]).trim();
                    if (remarks.indexOf("[")>-1 && remarks.indexOf("]")>-1) {
                        provider = remarks.substring(remarks.indexOf("[")+1, remarks.indexOf("]"));
                    }

                    Listitem li = new Listitem();

                    li.appendChild(new Listcell(claimType));
                    li.appendChild(new Listcell(provider));
                    li.appendChild(new Listcell(diagnosis));
                    li.appendChild(new Listcell(Libs.getICDByCode(diagnosis)));
                    li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[14])), "#,###.##"));
                    li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[15])), "#,###.##"));
                    li.appendChild(new Listcell(String.valueOf(Libs.getDiffDays(new SimpleDateFormat("yyyy-MM-dd").parse(serviceIn), new SimpleDateFormat("yyyy-MM-dd").parse(serviceOut)))));
                    li.appendChild(new Listcell((serviceIn.startsWith("0")) ? "" : serviceIn));
                    li.appendChild(new Listcell((serviceOut.startsWith("0")) ? "" : serviceOut));
                    li.appendChild(new Listcell(planString));

                    ClaimPOJO claimPOJO = new ClaimPOJO();
                    claimPOJO.setClaim_number(Libs.nn(o[12]).trim());
                    claimPOJO.setPolicy_number(policy.getYear() + "-" + policy.getBr() + "-" + policy.getDist() + "-" + policy.getPolicy_number());
                    claimPOJO.setIndex(Libs.nn(o[18]) + "-" + Libs.nn(o[16]));
                    claimPOJO.setClaim_count(Integer.valueOf(Libs.nn(o[13])));

                    li.setValue(claimPOJO);
                    lbClaimHistory.appendChild(li);
                }

//                Policy Claim History
                Listitem li = new Listitem();

                policyUsage += Double.valueOf(Libs.nn(o[15]));

                li.appendChild(new Listcell(claimType));
                li.appendChild(new Listcell(Libs.nn(o[17]).trim()));
                li.appendChild(new Listcell(Libs.nn(o[1]).trim()));
                li.appendChild(new Listcell(diagnosis));
                li.appendChild(new Listcell(Libs.getICDByCode(diagnosis)));
                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[14])), "#,###.##"));
                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[15])), "#,###.##"));
                li.appendChild(new Listcell(String.valueOf(Libs.getDiffDays(new SimpleDateFormat("yyyy-MM-dd").parse(serviceIn), new SimpleDateFormat("yyyy-MM-dd").parse(serviceOut)))));
                li.appendChild(new Listcell((serviceIn.startsWith("0")) ? "" : serviceIn));
                li.appendChild(new Listcell((serviceOut.startsWith("0")) ? "" : serviceOut));
                li.appendChild(new Listcell(planString));

                ClaimPOJO claimPOJO = new ClaimPOJO();
                claimPOJO.setClaim_number(Libs.nn(o[12]).trim());
                claimPOJO.setPolicy_number(policy.getYear() + "-" + policy.getBr() + "-" + policy.getDist() + "-" + policy.getPolicy_number());
                claimPOJO.setIndex(Libs.nn(o[18]) + "-" + Libs.nn(o[16]));
                claimPOJO.setClaim_count(Integer.valueOf(Libs.nn(o[13])));

                li.setValue(claimPOJO);

                lbPolicyClaimHistory.appendChild(li);
            }

            if (memberClaimCount>0) tabClaimHistory.setDisabled(false);
            if (policyClaimCount>0) tabPolicyClaimHistory.setDisabled(false);

            ftrApproved.setLabel(new DecimalFormat("#,###.##").format(memberUsage));
            ftrPolicyApproved.setLabel(new DecimalFormat("#,###.##").format(policyUsage));
        } catch (Exception ex) {
            log.error("populateClaimHistory", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void populatePlanRegistered() {
        lbPlan.getItems().clear();

        if (!member.getIp().isEmpty()) {
            lbPlan.appendChild(createPlanLine("INPATIENT", member.getIp(), member.getPlan_entry_date().get(0), member.getPlan_exit_date().get(0)));

            BenefitPOJO benefitPOJO = Libs.getBenefit(policy.getYear() + "-" + policy.getBr() + "-" + policy.getDist() + "-" + policy.getPolicy_number(), member.getIp());
            familyLimit = benefitPOJO.getLimit();
            familyLimitPlan = member.getIp();

            displayPlanItems(member.getIp());
        }

        if (!member.getOp().isEmpty()) {
            lbPlan.appendChild(createPlanLine("OUTPATIENT", member.getOp(), member.getPlan_entry_date().get(1), member.getPlan_exit_date().get(1)));
        }

        if (!member.getMaternity().isEmpty()) {
            lbPlan.appendChild(createPlanLine("MATERNITY", member.getMaternity(), member.getPlan_entry_date().get(2), member.getPlan_exit_date().get(2)));
        }

        if (!member.getDental().isEmpty()) {
            lbPlan.appendChild(createPlanLine("DENTAL", member.getDental(), member.getPlan_entry_date().get(3), member.getPlan_exit_date().get(3)));
        }

        if (!member.getGlasses().isEmpty()) {
            lbPlan.appendChild(createPlanLine("GLASSES", member.getGlasses(), member.getPlan_entry_date().get(4), member.getPlan_exit_date().get(4)));
        }
    }

    private Listitem createPlanLine(String type, String plan, String entryDate, String exitDate) {
        int planClaimCount = 0;

        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select count(*) "
                    + "from idnhltpf.dbo.hltclm "
                    + "where "
                    + "hclmyy=" + policy.getYear() + " and hclmpono=" + policy.getPolicy_number() + " "
                    + "and hclmidxno=" + member.getIdx() + " and hclmseqno='" + member.getSeq() + "' "
                    + "and (hclmpcode1 + hclmpcode2)='" + plan + "' "
                    + "and hclmrecid<>'C' ";

            planClaimCount = (Integer) s.createSQLQuery(qry).uniqueResult();
        } catch (Exception ex) {
            log.error("createPlanLine", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }

        String planString = clientPlanMap.get(plan);
        if (planString==null) planString = plan;

        Listitem li = new Listitem();
        li.appendChild(new Listcell(type));
        li.appendChild(new Listcell(planString));
        li.appendChild(new Listcell(entryDate.equals("0-0-0") ? "" : entryDate));
        li.appendChild(new Listcell(exitDate.equals("0-0-0") ? "" : exitDate));
        li.appendChild(Libs.createNumericListcell(planClaimCount, "#,###"));
        return li;
    }

    public void showMemberDetail() {
        familyLimit = 0;
        familyLimitPlan = "";
        memberUsage = 0;
        policyUsage = 0;

        tabClaimHistory.setDisabled(true);
        tabPolicyClaimHistory.setDisabled(true);

        ((Label) getFellow("lRemainingLimit")).setValue("0");
        member = lbRelatives.getSelectedItem().getValue();
        populate();
        ((Tabbox) getFellow("tbx")).setSelectedIndex(0);
    }

    public void showClaimDetail() {
        Window w = (Window) Executions.createComponents("views/ClaimDetail.zul", this, null);
        w.setAttribute("claim", lbClaimHistory.getSelectedItem().getValue());
        w.doModal();
    }

    public void showPolicyClaimDetail() {
        Window w = (Window) Executions.createComponents("views/ClaimDetail.zul", this, null);
        w.setAttribute("claim", lbPolicyClaimHistory.getSelectedItem().getValue());
        w.doModal();
    }

    private void displayPlanItems(String plan) {
        lbPlanItems.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + Libs.createListFieldString("a.hbftbcd") + ", "
                    + Libs.createListFieldString("a.hbftbpln") + " "
                    + "from idnhltpf.dbo.hltbft a "
                    + "where "
                    + "a.hbftyy=" + policy.getYear() + " and "
                    + "a.hbftpono=" + policy.getPolicy_number() + " and "
                    + "a.hbftcode='" + plan + "' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                for (int i=0; i<30; i++) {
                    if (!Libs.nn(o[i]).trim().isEmpty()) {
                        Listitem li = new Listitem();
                        li.setValue(o);

                        li.appendChild(new Listcell(Libs.nn(o[i]).trim()));
                        li.appendChild(new Listcell(Libs.getBenefitItemDescription(Libs.nn(o[i]).trim())));
                        li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[i+30])), "#,###.##"));
                        li.appendChild(new Listcell(""));

                        lbPlanItems.appendChild(li);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("displayPlanItems", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void populateUpdation() {
        lbUpdation.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "dt, type, parameters "
                    + "from ocisv3.dbo.updation_log "
                    + "where policy_number='" + policy.getPolicy_string() + "' "
                    + "order by dt desc ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                int type = Integer.valueOf(Libs.nn(o[1]));
                String stype = "";

                switch (type) {
                    case 1:
                        stype = "ADD CLAIM";
                        break;
                }

                Listitem li = new Listitem();
                li.appendChild(new Listcell(Libs.nn(o[0]).substring(0, 19)));
                li.appendChild(new Listcell(stype));
                li.appendChild(new Listcell(Libs.nn(o[2])));
                lbUpdation.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populateUpdation", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

}
