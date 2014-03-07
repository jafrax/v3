package com.imc.ocisv3.controllers;

import com.imc.ocisv3.pojos.ClaimPOJO;
import com.imc.ocisv3.pojos.MemberPOJO;
import com.imc.ocisv3.pojos.PolicyPOJO;
import com.imc.ocisv3.tools.Libs;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by faizal on 10/25/13.
 */
public class RejectedPendingClaimListController extends Window {

    private Logger log = LoggerFactory.getLogger(RejectedPendingClaimListController.class);
    private Listbox lb;
    private Paging pg;
    private Combobox cbPolicy;
    private String where;
    private String queryString;
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

            if (!Libs.nn(userProductViewrestriction).isEmpty()) {
                if (!userProductViewrestriction.contains(s.split("\\-")[3])) show=false;
                else show=true;
            }

            if (show) cbPolicy.appendItem(policyName + " (" + s + ")");
        }
    }

    private void populateCount() {
        Session s = Libs.sfDB.openSession();
        try {
            String countSelect = "select count(*) ";

            String qry = "from idnhltpf.dbo.hltclm a "
                    + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hclmyy and b.hhdrpono=a.hclmpono "
                    + "where "
                    + "b.hhdrinsid='" + Libs.getInsuranceId() + "' "
                    + "and a.hclmrecid in ('D', 'R') ";

            if (!Libs.nn(userProductViewrestriction).isEmpty()) qry += "and b.hhdrpono in (" + userProductViewrestriction + ") ";

            if (where!=null) qry += "and (" + where + ") ";

            if (cbPolicy.getSelectedIndex()>0) {
                String policy = cbPolicy.getSelectedItem().getLabel();
                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
                qry += "and (convert(varchar,a.hclmyy)+'-'+convert(varchar,a.hclmbr)+'-'+convert(varchar,a.hclmdist)+'-'+convert(varchar,a.hclmpono)='" + policy + "') ";
            }

            Integer count = (Integer) s.createSQLQuery(countSelect + qry).uniqueResult();
            pg.setTotalSize(count);
        } catch (Exception ex) {
            log.error("populateCount", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void populateCountForQuickSearch() {
        lb.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String countSelect = "select count(*) ";

            String qry = "from idnhltpf.dbo.hltclm a "
                    + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hclmyy and b.hhdrpono=a.hclmpono "
                    + "inner join idnhltpf.dbo.hltdt1 c on c.hdt1yy=a.hclmyy and c.hdt1pono=a.hclmpono and c.hdt1idxno=a.hclmidxno and c.hdt1seqno=a.hclmseqno and c.hdt1ctr=0 "
                    + "inner join idnhltpf.dbo.hltpro d on d.hpronomor=a.hclmnhoscd "
                    + "inner join idnhltpf.dbo.hltemp e on e.hempyy=a.hclmyy and e.hemppono=a.hclmpono and e.hempidxno=a.hclmidxno and e.hempseqno=a.hclmseqno and e.hempctr=0 "
                    + "where "
                    + "b.hhdrinsid='" + Libs.getInsuranceId() + "' "
                    + "and a.hclmrecid in ('D', 'R') ";

            if (!Libs.nn(userProductViewrestriction).isEmpty()) qry += "and b.hhdrpono in (" + userProductViewrestriction + ") ";

            if (where!=null) qry += "and (" + where + ") ";

            if (cbPolicy.getSelectedIndex()>0) {
                String policy = cbPolicy.getSelectedItem().getLabel();
                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
                qry += "and (convert(varchar,a.hclmyy)+'-'+convert(varchar,a.hclmbr)+'-'+convert(varchar,a.hclmdist)+'-'+convert(varchar,a.hclmpono)='" + policy + "') ";
            }

            Integer count = (Integer) s.createSQLQuery(countSelect + qry).uniqueResult();
            pg.setTotalSize(count);
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
                    + "a.hclmcno, a.hclmyy, a.hclmbr, a.hclmdist, a.hclmpono, "
                    + "b.hhdrname, a.hclmidxno, a.hclmseqno, "
                    + "c.hdt1name, a.hclmtclaim, "
                    + "(" + Libs.getProposed() + ") as proposed, " //10
                    + "(" + Libs.getApproved() + ") as approved, "
                    + "d.hproname, "
                    + "a.hclmcount, "
                    + "e.hempcnpol, e.hempcnid, "
                    + "'' as blank1, "
                    + "c.hdt1ncard, "
                    + "c.hdt1bdtyy, c.hdt1bdtmm, c.hdt1bdtdd, "
                    + "c.hdt1sex, " //21
                    + "f.hdt2plan1, f.hdt2plan2, f.hdt2plan3, f.hdt2plan4, f.hdt2plan5, f.hdt2plan6, "
                    + "f.hdt2sdtyy, f.hdt2sdtmm, f.hdt2sdtdd, "
                    + "f.hdt2mdtyy, f.hdt2mdtmm, f.hdt2mdtdd, "
                    + "(convert(varchar,f.hdt2pedty1)+'-'+convert(varchar,f.hdt2pedtm1)+'-'+convert(varchar,f.hdt2pedtd1)) as hdt2pedt1,"
                    + "(convert(varchar,f.hdt2pedty2)+'-'+convert(varchar,f.hdt2pedtm2)+'-'+convert(varchar,f.hdt2pedtd2)) as hdt2pedt2,"
                    + "(convert(varchar,f.hdt2pedty3)+'-'+convert(varchar,f.hdt2pedtm3)+'-'+convert(varchar,f.hdt2pedtd3)) as hdt2pedt3,"
                    + "(convert(varchar,f.hdt2pedty4)+'-'+convert(varchar,f.hdt2pedtm4)+'-'+convert(varchar,f.hdt2pedtd4)) as hdt2pedt4,"
                    + "(convert(varchar,f.hdt2pedty5)+'-'+convert(varchar,f.hdt2pedtm5)+'-'+convert(varchar,f.hdt2pedtd5)) as hdt2pedt5,"
                    + "(convert(varchar,f.hdt2pedty6)+'-'+convert(varchar,f.hdt2pedtm6)+'-'+convert(varchar,f.hdt2pedtd6)) as hdt2pedt6,"
                    + "(convert(varchar,f.hdt2pxdty1)+'-'+convert(varchar,f.hdt2pxdtm1)+'-'+convert(varchar,f.hdt2pxdtd1)) as hdt2pxdt1,"
                    + "(convert(varchar,f.hdt2pxdty2)+'-'+convert(varchar,f.hdt2pxdtm2)+'-'+convert(varchar,f.hdt2pxdtd2)) as hdt2pxdt2,"
                    + "(convert(varchar,f.hdt2pxdty3)+'-'+convert(varchar,f.hdt2pxdtm3)+'-'+convert(varchar,f.hdt2pxdtd3)) as hdt2pxdt3,"
                    + "(convert(varchar,f.hdt2pxdty4)+'-'+convert(varchar,f.hdt2pxdtm4)+'-'+convert(varchar,f.hdt2pxdtd4)) as hdt2pxdt4,"
                    + "(convert(varchar,f.hdt2pxdty5)+'-'+convert(varchar,f.hdt2pxdtm5)+'-'+convert(varchar,f.hdt2pxdtd5)) as hdt2pxdt5,"
                    + "(convert(varchar,f.hdt2pxdty6)+'-'+convert(varchar,f.hdt2pxdtm6)+'-'+convert(varchar,f.hdt2pxdtd6)) as hdt2pxdt6, "
                    + "c.hdt1mstat, "
                    + "g.hmem2data1, g.hmem2data2, g.hmem2data3, g.hmem2data4, "
                    + "a.hclmcdatey, a.hclmcdatem, a.hclmcdated, "
                    + "a.hclmsinyy, a.hclmsinmm, a.hclmsindd, "
                    + "a.hclmsoutyy, a.hclmsoutmm, a.hclmsoutdd, "
                    + "a.hclmrdatey, a.hclmrdatem, a.hclmrdated, "
                    + "a.hclmpdatey, a.hclmpdatem, a.hclmpdated, "
                    + "a.hclmdiscd1, a.hclmdiscd2, a.hclmdiscd3, "
                    + "a.hclmrecid ";

            String qry = "from idnhltpf.dbo.hltclm a "
                    + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hclmyy and b.hhdrpono=a.hclmpono "
                    + "inner join idnhltpf.dbo.hltdt1 c on c.hdt1yy=a.hclmyy and c.hdt1pono=a.hclmpono and c.hdt1idxno=a.hclmidxno and c.hdt1seqno=a.hclmseqno and c.hdt1ctr=0 "
                    + "inner join idnhltpf.dbo.hltpro d on d.hpronomor=a.hclmnhoscd "
                    + "inner join idnhltpf.dbo.hltemp e on e.hempyy=a.hclmyy and e.hemppono=a.hclmpono and e.hempidxno=a.hclmidxno and e.hempseqno=a.hclmseqno and e.hempctr=0 "
                    + "inner join idnhltpf.dbo.hltdt2 f on f.hdt2yy=a.hclmyy and f.hdt2pono=a.hclmpono and f.hdt2idxno=a.hclmidxno and f.hdt2seqno=a.hclmseqno and f.hdt2ctr=0 "
                    + "left outer join idnhltpf.dbo.hltmemo2 g on g.hmem2yy=a.hclmyy and g.hmem2pono=a.hclmpono and g.hmem2idxno=a.hclmidxno and g.hmem2seqno=a.hclmseqno and g.hmem2claim=a.hclmtclaim and g.hmem2count=a.hclmcount "
                    + "where "
                    + "b.hhdrinsid='" + Libs.getInsuranceId() + "' "
                    + "and a.hclmrecid in ('D', 'R') ";

            if (!Libs.nn(userProductViewrestriction).isEmpty()) qry += "and b.hhdrpono in (" + userProductViewrestriction + ") ";

            if (where!=null) qry += "and (" + where + ") ";

            if (cbPolicy.getSelectedIndex()>0) {
                String policy = cbPolicy.getSelectedItem().getLabel();
                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
                qry += "and (convert(varchar,a.hclmyy)+'-'+convert(varchar,a.hclmbr)+'-'+convert(varchar,a.hclmdist)+'-'+convert(varchar,a.hclmpono)='" + policy + "') ";
            }

            String order = "order by convert(date,convert(varchar,a.hclmcdated)+'-'+convert(varchar,a.hclmcdatem)+'-'+convert(varchar,a.hclmcdatey),105) desc ";

            queryString = select + qry + order;

            List<Object[]> l = s.createSQLQuery(select + qry + order).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                String policyName = Libs.nn(o[5]);
                if (Libs.config.get("demo_mode").equals("true") && Libs.getInsuranceId().equals("00051")) policyName = Libs.nn(Libs.config.get("demo_name"));

                String remarks = Libs.nn(o[47]).trim();
                String provider = Libs.nn(o[12]).trim();
                if (remarks.indexOf("[")>-1 && remarks.indexOf("]")>-1) {
                    provider = remarks.substring(remarks.indexOf("[")+1, remarks.indexOf("]"));
                }

                Listitem li = new Listitem();

                li.appendChild(new Listcell(Libs.nn(o[0])));
                li.appendChild(new Listcell(Libs.nn(o[14]).trim()));
                li.appendChild(new Listcell(policyName));
                li.appendChild(new Listcell(o[6] + "-" + o[7]));
                li.appendChild(new Listcell(Libs.nn(o[8])));
                li.appendChild(new Listcell(Libs.getStatus(Libs.nn(o[69]))));
                li.appendChild(new Listcell(Libs.getClaimType(Libs.nn(o[9]))));
                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[10])), "#,###.##"));
                li.appendChild(new Listcell(provider));
                li.appendChild(new Listcell(Libs.nn(o[47]).trim() + Libs.nn(o[48]).trim() + Libs.nn(o[49]).trim() + Libs.nn(o[50]).trim()));

                lb.appendChild(li);

                PolicyPOJO policyPOJO = new PolicyPOJO();
                policyPOJO.setYear(Integer.valueOf(Libs.nn(o[1])));
                policyPOJO.setBr(Integer.valueOf(Libs.nn(o[2])));
                policyPOJO.setDist(Integer.valueOf(Libs.nn(o[3])));
                policyPOJO.setPolicy_number(Integer.valueOf(Libs.nn(o[4])));
                policyPOJO.setName(Libs.nn(o[16]).trim());

                MemberPOJO memberPOJO = new MemberPOJO();
                memberPOJO.setPolicy(policyPOJO);
                memberPOJO.setName(Libs.nn(o[8]).trim());
                memberPOJO.setCard_number(Libs.nn(o[17]).trim());
                memberPOJO.setDob(Libs.nn(o[18]) + "-" + Libs.nn(o[19]) + "-" + Libs.nn(o[20]));
                memberPOJO.setStarting_date(Libs.nn(o[28]) + "-" + Libs.nn(o[29]) + "-" + Libs.nn(o[30]));
                memberPOJO.setMature_date(Libs.nn(o[31]) + "-" + Libs.nn(o[32]) + "-" + Libs.nn(o[33]));
                memberPOJO.setSex(Libs.nn(o[21]));
                memberPOJO.setIp(Libs.nn(o[22]).trim());
                memberPOJO.setOp(Libs.nn(o[23]).trim());
                memberPOJO.setMaternity(Libs.nn(o[24]).trim());
                memberPOJO.setDental(Libs.nn(o[25]).trim());
                memberPOJO.setGlasses(Libs.nn(o[26]).trim());
                memberPOJO.setMarital_status(Libs.nn(o[46]));
                memberPOJO.setIdx(Libs.nn(o[6]));
                memberPOJO.setSeq(Libs.nn(o[7]));
                memberPOJO.setClient_policy_number(Libs.nn(o[14]).trim());
                memberPOJO.setClient_id_number(Libs.nn(o[15]).trim());
                memberPOJO.getPlan_entry_date().add(Libs.nn(o[34]));
                memberPOJO.getPlan_entry_date().add(Libs.nn(o[35]));
                memberPOJO.getPlan_entry_date().add(Libs.nn(o[36]));
                memberPOJO.getPlan_entry_date().add(Libs.nn(o[37]));
                memberPOJO.getPlan_entry_date().add(Libs.nn(o[38]));
                memberPOJO.getPlan_entry_date().add(Libs.nn(o[39]));
                memberPOJO.getPlan_exit_date().add(Libs.nn(o[40]));
                memberPOJO.getPlan_exit_date().add(Libs.nn(o[41]));
                memberPOJO.getPlan_exit_date().add(Libs.nn(o[42]));
                memberPOJO.getPlan_exit_date().add(Libs.nn(o[43]));
                memberPOJO.getPlan_exit_date().add(Libs.nn(o[44]));
                memberPOJO.getPlan_exit_date().add(Libs.nn(o[45]));

                ClaimPOJO claimPOJO = new ClaimPOJO();
                claimPOJO.setPolicy(policyPOJO);
                claimPOJO.setMember(memberPOJO);
                claimPOJO.setClaim_number(Libs.nn(o[0]).trim());
                claimPOJO.setPolicy_number(Libs.nn(o[1]) + '-' + Libs.nn(o[2]) + '-' + Libs.nn(o[3]) + '-' + Libs.nn(o[4]));
                claimPOJO.setIndex(Libs.nn(o[6]) + '-' + Libs.nn(o[7]).trim());
                claimPOJO.setClaim_count(Integer.valueOf(Libs.nn(o[13])));

                li.setValue(claimPOJO);

                ((Toolbarbutton) getFellow("tbnShowMemberDetail")).setDisabled(true);
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
        if (!val.isEmpty()) {
            where = "convert(varchar,c.hdt1ncard) like '%" + val + "%' or "
                    + "c.hdt1name like '%" + val + "%' or "
                    + "e.hempcnpol like '%" + val + "%' or "
                    + "e.hempcnid like '%" + val + "%' or "
                    + "a.hclmcno like '%" + val + "%' ";

            populateCountForQuickSearch();
            populate(0, pg.getPageSize());
        } else refresh();
    }

    public void lbSelected() {
        if (lb.getSelectedCount()>0) {
            ((Toolbarbutton) getFellow("tbnShowMemberDetail")).setDisabled(false);
        }
    }

    public void showClaimDetail() {
        Window w = (Window) Executions.createComponents("views/ClaimDetail.zul", this, null);
        w.setAttribute("claim", lb.getSelectedItem().getValue());
        w.doModal();
    }

    public void policySelected() {
        quickSearch();
    }

    public void showMemberDetail() {
        ClaimPOJO claimPOJO = lb.getSelectedItem().getValue();
        Window w = (Window) Executions.createComponents("views/MemberDetail.zul", Libs.getRootWindow(), null);
        w.setAttribute("policy", claimPOJO.getPolicy());
        w.setAttribute("member", claimPOJO.getMember());
        w.doModal();
    }

    public void export() {
        Window w = (Window) Executions.createComponents("views/ReportDialog.zul", this, null);
        w.setAttribute("title", "Claim History");
        w.setAttribute("product", cbPolicy.getSelectedItem().getLabel());
        w.doModal();

        if (w.getAttribute("export")!=null && w.getAttribute("export").equals(true)) {
            int scope = (Integer) w.getAttribute("scope");

            if (scope==0) {
                Session s = Libs.sfDB.openSession();
                try {
                    List<Object[]> l = s.createSQLQuery(queryString).list();
                    createReport(l);
                } catch (Exception ex) {
                    log.error("export", ex);
                } finally {
                    if (s!=null && s.isOpen()) s.close();
                }
            } else {
                Session s = Libs.sfDB.openSession();
                try {
                    String productName = String.valueOf(w.getAttribute("product"));
                    int period = (Integer) w.getAttribute("period");

                    String qry = "select "
                            + "a.hclmcno, a.hclmyy, a.hclmbr, a.hclmdist, a.hclmpono, b.hhdrname, a.hclmidxno, a.hclmseqno, c.hdt1name, a.hclmtclaim, "
                            + "(" + Libs.getProposed() + ") as proposed, " //10
                            + "(" + Libs.getApproved() + ") as approved, "
                            + "d.hproname, a.hclmcount, e.hempcnpol, e.hempcnid, '' as blank1, c.hdt1ncard, c.hdt1bdtyy, c.hdt1bdtmm, c.hdt1bdtdd, c.hdt1sex, " //21
                            + "f.hdt2plan1, f.hdt2plan2, f.hdt2plan3, f.hdt2plan4, f.hdt2plan5, f.hdt2plan6, "
                            + "f.hdt2sdtyy, f.hdt2sdtmm, f.hdt2sdtdd, "
                            + "f.hdt2mdtyy, f.hdt2mdtmm, f.hdt2mdtdd, "
                            + "(convert(varchar,f.hdt2pedty1)+'-'+convert(varchar,f.hdt2pedtm1)+'-'+convert(varchar,f.hdt2pedtd1)) as hdt2pedt1,"
                            + "(convert(varchar,f.hdt2pedty2)+'-'+convert(varchar,f.hdt2pedtm2)+'-'+convert(varchar,f.hdt2pedtd2)) as hdt2pedt2,"
                            + "(convert(varchar,f.hdt2pedty3)+'-'+convert(varchar,f.hdt2pedtm3)+'-'+convert(varchar,f.hdt2pedtd3)) as hdt2pedt3,"
                            + "(convert(varchar,f.hdt2pedty4)+'-'+convert(varchar,f.hdt2pedtm4)+'-'+convert(varchar,f.hdt2pedtd4)) as hdt2pedt4,"
                            + "(convert(varchar,f.hdt2pedty5)+'-'+convert(varchar,f.hdt2pedtm5)+'-'+convert(varchar,f.hdt2pedtd5)) as hdt2pedt5,"
                            + "(convert(varchar,f.hdt2pedty6)+'-'+convert(varchar,f.hdt2pedtm6)+'-'+convert(varchar,f.hdt2pedtd6)) as hdt2pedt6,"
                            + "(convert(varchar,f.hdt2pxdty1)+'-'+convert(varchar,f.hdt2pxdtm1)+'-'+convert(varchar,f.hdt2pxdtd1)) as hdt2pxdt1,"
                            + "(convert(varchar,f.hdt2pxdty2)+'-'+convert(varchar,f.hdt2pxdtm2)+'-'+convert(varchar,f.hdt2pxdtd2)) as hdt2pxdt2,"
                            + "(convert(varchar,f.hdt2pxdty3)+'-'+convert(varchar,f.hdt2pxdtm3)+'-'+convert(varchar,f.hdt2pxdtd3)) as hdt2pxdt3,"
                            + "(convert(varchar,f.hdt2pxdty4)+'-'+convert(varchar,f.hdt2pxdtm4)+'-'+convert(varchar,f.hdt2pxdtd4)) as hdt2pxdt4,"
                            + "(convert(varchar,f.hdt2pxdty5)+'-'+convert(varchar,f.hdt2pxdtm5)+'-'+convert(varchar,f.hdt2pxdtd5)) as hdt2pxdt5,"
                            + "(convert(varchar,f.hdt2pxdty6)+'-'+convert(varchar,f.hdt2pxdtm6)+'-'+convert(varchar,f.hdt2pxdtd6)) as hdt2pxdt6, "
                            + "c.hdt1mstat, "
                            + "g.hmem2data1, g.hmem2data2, g.hmem2data3, g.hmem2data4, "
                            + "a.hclmcdatey, a.hclmcdatem, a.hclmcdated, "
                            + "a.hclmsinyy, a.hclmsinmm, a.hclmsindd, "
                            + "a.hclmsoutyy, a.hclmsoutmm, a.hclmsoutdd, "
                            + "a.hclmrdatey, a.hclmrdatem, a.hclmrdated, "
                            + "a.hclmpdatey, a.hclmpdatem, a.hclmpdated, "
                            + "a.hclmdiscd1, a.hclmdiscd2, a.hclmdiscd3, "
                            + "a.hclmrecid "
                            + "from idnhltpf.dbo.hltclm a "
                            + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hclmyy and b.hhdrpono=a.hclmpono "
                            + "inner join idnhltpf.dbo.hltdt1 c on c.hdt1yy=a.hclmyy and c.hdt1pono=a.hclmpono and c.hdt1idxno=a.hclmidxno and c.hdt1seqno=a.hclmseqno and c.hdt1ctr=0 "
                            + "inner join idnhltpf.dbo.hltpro d on d.hpronomor=a.hclmnhoscd "
                            + "inner join idnhltpf.dbo.hltemp e on e.hempyy=a.hclmyy and e.hemppono=a.hclmpono and e.hempidxno=a.hclmidxno and e.hempseqno=a.hclmseqno and e.hempctr=0 "
                            + "inner join idnhltpf.dbo.hltdt2 f on f.hdt2yy=a.hclmyy and f.hdt2pono=a.hclmpono and f.hdt2idxno=a.hclmidxno and f.hdt2seqno=a.hclmseqno and f.hdt2ctr=0 "
                            + "left outer join idnhltpf.dbo.hltmemo2 g on g.hmem2yy=a.hclmyy and g.hmem2pono=a.hclmpono and g.hmem2idxno=a.hclmidxno and g.hmem2seqno=a.hclmseqno and g.hmem2claim=a.hclmtclaim and g.hmem2count=a.hclmcount "
                            + "where "
                            + "b.hhdrinsid='" + Libs.getInsuranceId() + "' "
                            + "and a.hclmrecid in ('R', 'D') ";

                    if (!Libs.nn(userProductViewrestriction).isEmpty()) qry += "and b.hhdrpono in (" + userProductViewrestriction + ") ";

                    if (!productName.toLowerCase().equals("all products")) {
                        String policy = productName.substring(productName.indexOf("(")+1, productName.indexOf(")"));
                        qry += "and (convert(varchar,a.hclmyy)+'-'+convert(varchar,a.hclmbr)+'-'+convert(varchar,a.hclmdist)+'-'+convert(varchar,a.hclmpono)='" + policy + "') ";
                    }

                    switch (period) {
                        case 0:
                            Date date = (Date) w.getAttribute("date");
                            java.util.Calendar cal = java.util.Calendar.getInstance();
                            cal.setTime(date);
                            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
                            cal.set(java.util.Calendar.MONTH, 0);
                            qry += "and convert(datetime,convert(varchar,hclmcdated)+'-'+convert(varchar,hclmcdatem)+'-'+convert(varchar,hclmcdatey),105) between '" + new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()) + "' and '" + new SimpleDateFormat("yyyy-MM-dd").format(date) + "'";
                            break;
                        case 1:
                            break;
                        case 2:
                            Date dateStart = (Date) w.getAttribute("dateStart");
                            Date dateEnd = (Date) w.getAttribute("dateEnd");
                            qry += "and convert(datetime,convert(varchar,hclmcdated)+'-'+convert(varchar,hclmcdatem)+'-'+convert(varchar,hclmcdatey),105) between '" + new SimpleDateFormat("yyyy-MM-dd").format(dateStart) + "' and '" + new SimpleDateFormat("yyyy-MM-dd").format(dateEnd) + "'";
                            break;
                        case 3:
                            int monthStart = (Integer) w.getAttribute("monthStart");
                            int monthEnd = (Integer) w.getAttribute("monthEnd");
                            int monthYearStart = (Integer) w.getAttribute("monthYearStart");
                            int monthYearEnd = (Integer) w.getAttribute("monthYearEnd");
                            int maxDay = java.util.Calendar.getInstance().getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
                            java.util.Calendar calStart = java.util.Calendar.getInstance();
                            java.util.Calendar calEnd = java.util.Calendar.getInstance();
                            calStart.set(monthYearStart, monthStart, 1);
                            calEnd.set(monthYearEnd, monthEnd, maxDay);
                            qry += "and convert(datetime,convert(varchar,hclmcdated)+'-'+convert(varchar,hclmcdatem)+'-'+convert(varchar,hclmcdatey),105) between '" + new SimpleDateFormat("yyyy-MM-dd").format(calStart.getTime()) + "' and '" + new SimpleDateFormat("yyyy-MM-dd").format(calEnd.getTime()) + "'";
                            break;
                        case 4:
                            int yearStart = (Integer) w.getAttribute("yearStart");
                            int yearEnd = (Integer) w.getAttribute("yearEnd");
                            java.util.Calendar calStart1 = java.util.Calendar.getInstance();
                            java.util.Calendar calEnd1 = java.util.Calendar.getInstance();
                            calStart1.set(yearStart, 0, 1);
                            calEnd1.set(yearEnd, 11, 31);
                            qry += "and convert(datetime,convert(varchar,hclmcdated)+'-'+convert(varchar,hclmcdatem)+'-'+convert(varchar,hclmcdatey),105) between '" + new SimpleDateFormat("yyyy-MM-dd").format(calStart1.getTime()) + "' and '" + new SimpleDateFormat("yyyy-MM-dd").format(calEnd1.getTime()) + "'";
                            break;
                    }

                    List<Object[]> l = s.createSQLQuery(qry).list();
                    createReport(l);
                } catch (Exception ex) {
                    log.error("export", ex);
                } finally {
                    if (s!=null && s.isOpen()) s.close();
                }
            }
        }
    }

    private void createReport(List<Object[]> list) {
        String[] columnsMemberWise = new String[] {
                "POLICY YEAR", "BR", "DIST", "POLICY NUMBER", "COMPANY NAME", "INDEX", "SEQ", "CARD NUMBER",
                "NAME", "COUNT", "TYPE", "CLAIM-YEAR", "CLAIM-MONTH", "CLAIM-DAY", "SIN-YEAR", "SIN-MONTH",
                "SIN-DAY", "SOUT-YEAR", "SOUT-MONTH", "SOUT-DAY", "RECEIPT-YEAR", "RECEIPT-MONTH",
                "RECEIPT-DAY", "PAYMENT-YEAR", "PAYMENT-MONTH", "PAYMENT-DAY", "HID NUMBER", "PROVIDER NAME",
                "ICD1", "ICD2", "ICD3", "PROPOSED", "APPROVED", "STATUS", "MEMO" };

        try {
            Workbook wb = new HSSFWorkbook();
            Sheet sheet = wb.createSheet("Claim History");
            int cnt = 0;

            org.apache.poi.ss.usermodel.Row row = sheet.createRow(cnt);

            for (int i=0; i<columnsMemberWise.length; i++) {
                Libs.createCell(row, i, columnsMemberWise[i]);
            }

            cnt++;

            for (Object[] o : list) {
                row = sheet.createRow(cnt);

                Libs.createCell(row, 0, Libs.nn(o[1]));
                Libs.createCell(row, 1, Libs.nn(o[2]));
                Libs.createCell(row, 2, Libs.nn(o[3]));
                Libs.createCell(row, 3, Libs.nn(o[4]));
                Libs.createCell(row, 4, Libs.nn(o[5]));
                Libs.createCell(row, 5, Libs.nn(o[6]));
                Libs.createCell(row, 6, Libs.nn(o[7]));
                Libs.createCell(row, 7, Libs.nn(o[17]));
                Libs.createCell(row, 8, Libs.nn(o[8]));
                Libs.createCell(row, 9, Libs.nn(o[13]));
                Libs.createCell(row, 10, Libs.nn(o[9]));
                Libs.createCell(row, 11, Libs.nn(o[51]));
                Libs.createCell(row, 12, Libs.nn(o[52]));
                Libs.createCell(row, 13, Libs.nn(o[53]));
                Libs.createCell(row, 14, Libs.nn(o[54]));
                Libs.createCell(row, 15, Libs.nn(o[55]));
                Libs.createCell(row, 16, Libs.nn(o[56]));
                Libs.createCell(row, 17, Libs.nn(o[57]));
                Libs.createCell(row, 18, Libs.nn(o[58]));
                Libs.createCell(row, 19, Libs.nn(o[59]));
                Libs.createCell(row, 20, Libs.nn(o[60]));
                Libs.createCell(row, 21, Libs.nn(o[61]));
                Libs.createCell(row, 22, Libs.nn(o[62]));
                Libs.createCell(row, 23, Libs.nn(o[63]));
                Libs.createCell(row, 24, Libs.nn(o[64]));
                Libs.createCell(row, 25, Libs.nn(o[65]));
                Libs.createCell(row, 26, Libs.nn(o[0]));
                Libs.createCell(row, 27, Libs.nn(o[12]));
                Libs.createCell(row, 28, Libs.nn(o[66]));
                Libs.createCell(row, 29, Libs.nn(o[67]));
                Libs.createCell(row, 30, Libs.nn(o[68]));
                Libs.createCell(row, 31, o[10]);
                Libs.createCell(row, 32, o[11]);
                Libs.createCell(row, 33, Libs.getStatus(Libs.nn(o[69])));
                Libs.createCell(row, 34, Libs.nn(o[47]).trim() + Libs.nn(o[48]).trim() + Libs.nn(o[49]).trim() + Libs.nn(o[50]).trim());

                cnt++;
            }

            String fn = "ClaimHistory-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xls";

            FileOutputStream out = new FileOutputStream(Libs.config.get("temp_dir").toString() + File.separator + fn);
            wb.write(out);
            out.close();

            Thread.sleep(5000);

            File f = new File(Libs.config.get("temp_dir").toString() + File.separator + fn);
            InputStream is = new FileInputStream(f);
            Filedownload.save(is, "application/vnd.ms-excel", fn);
            f.delete();
        } catch (Exception ex) {
            log.error("createReport", ex);
        }
    }

}
