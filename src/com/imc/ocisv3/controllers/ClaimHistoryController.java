package com.imc.ocisv3.controllers;

import com.imc.ocisv3.pojos.ClaimPOJO;
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

import java.util.List;

/**
 * Created by faizal on 10/25/13.
 */
public class ClaimHistoryController extends Window {

    private Logger log = LoggerFactory.getLogger(ClaimHistoryController.class);
    private Listbox lb;
    private Paging pg;
    private Combobox cbPolicy;
    private String where;

    public void onCreate() {
        initComponents();
        populateCount();
        populate(0, pg.getPageSize());
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

        cbPolicy.appendItem("All Policies");
        cbPolicy.setSelectedIndex(0);
        for (String s : Libs.policyMap.keySet()) {
            String policyName = Libs.policyMap.get(s);
            if (Libs.config.get("demo_mode").equals("true") && Libs.insuranceId.equals("00051")) policyName = "HAS - P.T. Semesta Alam";

            cbPolicy.appendItem(policyName + " (" + s + ")");
        }
    }

    private void populateCount() {
        Session s = Libs.sfDB.openSession();
        try {
            String countSelect = "select count(*) ";

            String qry = "from idnhltpf.dbo.hltclm a "
                    + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hclmyy and b.hhdrpono=a.hclmpono "
                    + "where "
                    + "b.hhdrinsid='" + Libs.insuranceId + "' "
                    + "and a.hclmrecid<>'C' ";

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
                    + "b.hhdrinsid='" + Libs.insuranceId + "' "
                    + "and a.hclmrecid<>'C' ";

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
                    + "c.hdt1mstat ";

            String qry = "from idnhltpf.dbo.hltclm a "
                    + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hclmyy and b.hhdrpono=a.hclmpono "
                    + "inner join idnhltpf.dbo.hltdt1 c on c.hdt1yy=a.hclmyy and c.hdt1pono=a.hclmpono and c.hdt1idxno=a.hclmidxno and c.hdt1seqno=a.hclmseqno and c.hdt1ctr=0 "
                    + "inner join idnhltpf.dbo.hltpro d on d.hpronomor=a.hclmnhoscd "
                    + "inner join idnhltpf.dbo.hltemp e on e.hempyy=a.hclmyy and e.hemppono=a.hclmpono and e.hempidxno=a.hclmidxno and e.hempseqno=a.hclmseqno and e.hempctr=0 "
                    + "inner join idnhltpf.dbo.hltdt2 f on f.hdt2yy=a.hclmyy and f.hdt2pono=a.hclmpono and f.hdt2idxno=a.hclmidxno and f.hdt2seqno=a.hclmseqno and f.hdt2ctr=0 "
                    + "where "
                    + "b.hhdrinsid='" + Libs.insuranceId + "' "
                    + "and a.hclmrecid<>'C' ";

            if (where!=null) qry += "and (" + where + ") ";

            if (cbPolicy.getSelectedIndex()>0) {
                String policy = cbPolicy.getSelectedItem().getLabel();
                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
                qry += "and (convert(varchar,a.hclmyy)+'-'+convert(varchar,a.hclmbr)+'-'+convert(varchar,a.hclmdist)+'-'+convert(varchar,a.hclmpono)='" + policy + "') ";
            }

            String order = "order by convert(date,convert(varchar,a.hclmcdated)+'-'+convert(varchar,a.hclmcdatem)+'-'+convert(varchar,a.hclmcdatey),105) desc ";

            List<Object[]> l = s.createSQLQuery(select + qry + order).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                String policyName = Libs.nn(o[5]);
                if (Libs.config.get("demo_mode").equals("true") && Libs.insuranceId.equals("00051")) policyName = "HAS - P.T. Semesta Alam";

                Listitem li = new Listitem();

                li.appendChild(new Listcell(Libs.nn(o[0])));
                li.appendChild(new Listcell(Libs.nn(o[14]).trim()));
                li.appendChild(new Listcell(policyName));
                li.appendChild(new Listcell(o[6] + "-" + o[7]));
                li.appendChild(new Listcell(Libs.nn(o[8])));
                li.appendChild(new Listcell(""));
                li.appendChild(new Listcell(Libs.nn(o[9])));
                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[10])), "#,###.##"));
                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[11])), "#,###.##"));
                li.appendChild(new Listcell(Libs.nn(o[12]).trim()));

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
        Window w = (Window) Executions.createComponents("views/MemberDetail.zul", Libs.rootWindow, null);
        w.setAttribute("policy", claimPOJO.getPolicy());
        w.setAttribute("member", claimPOJO.getMember());
        w.doModal();
    }

}
