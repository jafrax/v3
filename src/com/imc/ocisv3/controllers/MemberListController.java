package com.imc.ocisv3.controllers;

import com.imc.ocisv3.pojos.BenefitPOJO;
import com.imc.ocisv3.pojos.MemberPOJO;
import com.imc.ocisv3.pojos.PolicyPOJO;
import com.imc.ocisv3.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by faizal on 10/30/13.
 */
public class MemberListController extends Window {

    private Logger log = LoggerFactory.getLogger(MemberListController.class);
    private Listbox lb;
    private Paging pg;
    private String where;
    private Combobox cbPolicy;
    private String userProductViewrestriction;

    public void onCreate() {
        if (!Libs.checkSession()) {
            userProductViewrestriction = Libs.restrictUserProductView.get(Libs.getUser());
            initComponents();
            populateCount();
            populate(0, pg.getPageSize());
        }
    }

    private void initComponents() {
        lb = (Listbox) getFellow("lb");
        pg = (Paging) getFellow("pg");
        cbPolicy = (Combobox) getFellow("cbPolicy");

        pg.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populate(evt.getActivePage()*pg.getPageSize(), pg.getPageSize());
            }
        });

        cbPolicy.appendItem("All Products");
        cbPolicy.setSelectedIndex(0);
        boolean show = true;
        for (String s : Libs.policyMap.keySet()) {
            String policyName = Libs.policyMap.get(s);
            if (Libs.config.get("demo_mode").equals("true") && Libs.getInsuranceId().equals("00051")) policyName = Libs.nn(Libs.config.get("demo_name"));

            String restriction = Libs.restrictUserProductView.get(Libs.getUser());
            if (!Libs.nn(restriction).isEmpty()) {
                if (!restriction.contains(s.split("\\-")[3])) show=false;
                else show=true;
            }

            if (show) cbPolicy.appendItem(policyName + " (" + s + ")");
        }
    }

    private void populateCount() {
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select count(*) "
                    + "from idnhltpf.dbo.hltdt1 a "
                    + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hdt1yy and b.hhdrpono=a.hdt1pono "
                    + "where "
                    + "b.hhdrinsid='" + Libs.getInsuranceId() + "' "
                    + "and a.hdt1ctr=0 ";

            if (where!=null) qry += "and (" + where + ") ";

            Integer recordsCount = (Integer) s.createSQLQuery(qry).uniqueResult();
            pg.setTotalSize(recordsCount);
        } catch (Exception ex) {
            log.error("populateCount", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void populateCountForQuickSearch() {
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select count(*) "
                    + "from idnhltpf.dbo.hltdt1 a "
                    + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hdt1yy and b.hhdrpono=a.hdt1pono "
                    + "inner join idnhltpf.dbo.hltdt2 d on d.hdt2yy=a.hdt1yy and d.hdt2pono=a.hdt1pono and d.hdt2idxno=a.hdt1idxno and d.hdt2seqno=a.hdt1seqno and d.hdt2ctr=a.hdt1ctr "
                    + "inner join idnhltpf.dbo.hltemp c on c.hempyy=a.hdt1yy and c.hemppono=a.hdt1pono and c.hempidxno=a.hdt1idxno and c.hempseqno=a.hdt1seqno and c.hempctr=a.hdt1ctr "
                    + "where "
                    + "b.hhdrinsid='" + Libs.getInsuranceId() + "' "
                    + "and a.hdt1ctr=0 ";

            if (where!=null) qry += "and (" + where + ") ";

            if (cbPolicy.getSelectedIndex()>0) {
                String policy = cbPolicy.getSelectedItem().getLabel();
                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
                qry += "and (convert(varchar,a.hdt1yy)+'-'+convert(varchar,a.hdt1br)+'-'+convert(varchar,a.hdt1dist)+'-'+convert(varchar,a.hdt1pono)='" + policy + "') ";
            }

            Integer recordsCount = (Integer) s.createSQLQuery(qry).uniqueResult();
            pg.setTotalSize(recordsCount);
        } catch (Exception ex) {
            log.error("populateCountForQuickSearch", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void populate(int offset, int limit) {
        lb.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String select = "select "
                    + "a.hdt1ncard, a.hdt1name, "
                    + "a.hdt1bdtyy, a.hdt1bdtmm, a.hdt1bdtdd, "
                    + "a.hdt1sex, "
                    + "b.hdt2plan1, b.hdt2plan2, b.hdt2plan3, b.hdt2plan4, b.hdt2plan5, b.hdt2plan6, "
                    + "b.hdt2sdtyy, b.hdt2sdtmm, b.hdt2sdtdd, "
                    + "b.hdt2mdtyy, b.hdt2mdtmm, b.hdt2mdtdd, "
                    + "a.hdt1idxno, a.hdt1seqno, "
                    + "c.hempcnid, " //20
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
                    + "a.hdt1mstat, c.hempcnpol, " //33
                    + "a.hdt1yy, a.hdt1br, a.hdt1dist, a.hdt1pono, "
                    + "d.hhdrname, "
                    + "b.hdt2moe, " //40
                    + "b.hdt2xdtyy, b.hdt2xdtmm, b.hdt2xdtdd ";

            String qry = "from idnhltpf.dbo.hltdt1 a "
                    + "inner join idnhltpf.dbo.hltdt2 b on b.hdt2yy=a.hdt1yy and b.hdt2pono=a.hdt1pono and b.hdt2idxno=a.hdt1idxno and b.hdt2seqno=a.hdt1seqno and b.hdt2ctr=a.hdt1ctr "
                    + "inner join idnhltpf.dbo.hltemp c on c.hempyy=a.hdt1yy and c.hemppono=a.hdt1pono and c.hempidxno=a.hdt1idxno and c.hempseqno=a.hdt1seqno and c.hempctr=a.hdt1ctr "
                    + "inner join idnhltpf.dbo.hlthdr d on d.hhdryy=a.hdt1yy and d.hhdrpono=a.hdt1pono "
                    + "where "
                    + "a.hdt1ctr=0 "
                    + "and d.hhdrinsid='" + Libs.getInsuranceId() + "' "
                    + "and a.hdt1idxno<>99999 ";

            if (!Libs.nn(userProductViewrestriction).isEmpty()) qry += "and d.hhdrpono in (" + userProductViewrestriction + ") ";

            if (where!=null) qry += "and (" + where + ") ";

            if (cbPolicy.getSelectedIndex()>0) {
                String policy = cbPolicy.getSelectedItem().getLabel();
                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
                qry += "and (convert(varchar,a.hdt1yy)+'-'+convert(varchar,a.hdt1br)+'-'+convert(varchar,a.hdt1dist)+'-'+convert(varchar,a.hdt1pono)='" + policy + "') ";
            }

            String order = "order by a.hdt1name asc ";

            List<Object[]> l = s.createSQLQuery(select + qry + order).setFirstResult(offset).setMaxResults(limit).list();

            for (Object[] o : l) {
                PolicyPOJO policyPOJO = new PolicyPOJO();
                policyPOJO.setYear(Integer.valueOf(Libs.nn(o[35])));
                policyPOJO.setBr(Integer.valueOf(Libs.nn(o[36])));
                policyPOJO.setDist(Integer.valueOf(Libs.nn(o[37])));
                policyPOJO.setPolicy_number(Integer.valueOf(Libs.nn(o[38])));
                policyPOJO.setName(Libs.nn(o[39]).trim());

                Map<String,String> clientPlanMap = Libs.getClientPlanMap(policyPOJO.getPolicy_string());

                MemberPOJO memberPOJO = new MemberPOJO();
                memberPOJO.setPolicy(policyPOJO);
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
                if (Libs.nn(o[40]).equals("U")) {
                    String effectiveDate = Libs.nn(o[41]) + "-" + Libs.nn(o[42]) + "-" + Libs.nn(o[43]);
                    int effectiveDays = Libs.getDiffDays(new Date(), new SimpleDateFormat("yyyy-MM-dd").parse(effectiveDate));
                    if (effectiveDays<0) {
                        lStatus.setValue("INACTIVE");
                        lStatus.setStyle("color:#000000");
                    }
                }

                BenefitPOJO benefitPOJO = Libs.getBenefit(policyPOJO.getYear() + "-" + policyPOJO.getBr() + "-" + policyPOJO.getDist() + "-" + policyPOJO.getPolicy_number(), memberPOJO.getIp());

                lcStatus.appendChild(lStatus);

                Listitem li = new Listitem();
                li.setValue(memberPOJO);

                li.appendChild(lcStatus);
                li.appendChild(new Listcell(memberPOJO.getName()));
                li.appendChild(new Listcell(Libs.nn(o[34]).trim()));
                li.appendChild(new Listcell(Libs.nn(o[20]).trim()));
                li.appendChild(new Listcell(memberPOJO.getCard_number()));
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

                if (policyPOJO.getPolicy_number()==51039) {
                    li.appendChild(Libs.createNumericListcell(Libs.getMemberClaimUsage(policyPOJO, memberPOJO), "#,###.##"));
                    li.appendChild(Libs.createNumericListcell(benefitPOJO.getLimit(), "#,###.##"));
                } else {
                    li.appendChild(new Listcell("-"));
                    li.appendChild(new Listcell("-"));
                }
                lb.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    public void refresh() {
        where = null;
        populateCount();
        populate(0, pg.getPageSize());
    }

    public void quickSearch() {
        String val = ((Textbox) getFellow("tQuickSearch")).getText();
        if (!val.isEmpty() || (val.isEmpty() && cbPolicy.getSelectedIndex()>0)) {
            where = "a.hdt1ncard like '%" + val + "%' or "
                    + "a.hdt1name like '%" + val + "%' or "
                    + "c.hempcnpol like '%" + val + "%' or "
                    + "c.hempcnid like '%" + val + "%' ";
            populateCountForQuickSearch();
            populate(0, pg.getPageSize());
        } else refresh();
    }

    public void showMemberDetail() {
        MemberPOJO memberPOJO = lb.getSelectedItem().getValue();
        Window w = (Window) Executions.createComponents("views/MemberDetail.zul", Libs.getRootWindow(), null);
        w.setAttribute("policy", memberPOJO.getPolicy());
        w.setAttribute("member", memberPOJO);
        w.doModal();
    }

    public void policySelected() {
        quickSearch();
    }

    public void export() {
        Libs.showDeveloping();
    }

}
