package com.imc.ocisv3.controllers;

import com.imc.ocisv3.pojos.ClaimPOJO;
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
                    + "(" + Libs.getProposed() + ") as proposed, "
                    + "(" + Libs.getApproved() + ") as approved, "
                    + "d.hproname, "
                    + "a.hclmcount, "
                    + "e.hempcnpol, e.hempcnid ";

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

                ClaimPOJO claimPOJO = new ClaimPOJO();
                claimPOJO.setClaim_number(Libs.nn(o[0]).trim());
                claimPOJO.setPolicy_number(Libs.nn(o[1]) + '-' + Libs.nn(o[2]) + '-' + Libs.nn(o[3]) + '-' + Libs.nn(o[4]));
                claimPOJO.setIndex(Libs.nn(o[6]) + '-' + Libs.nn(o[7]).trim());
                claimPOJO.setClaim_count(Integer.valueOf(Libs.nn(o[13])));

                li.setValue(claimPOJO);
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

    public void lbSelected() {}

    public void showClaimDetail() {
        Window w = (Window) Executions.createComponents("views/ClaimDetail.zul", this, null);
        w.setAttribute("claim", lb.getSelectedItem().getValue());
        w.doModal();
    }

    public void policySelected() {
        quickSearch();
    }

}
