package com.imc.ocisv3.controllers;

import com.imc.ocisv3.pojos.EDCTransactionPOJO;
import com.imc.ocisv3.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;

import java.util.List;
import java.util.Map;

/**
 * Created by faizal on 10/25/13.
 */
public class EDCTransactionsController extends Window {

    private Logger log = LoggerFactory.getLogger(EDCTransactionsController.class);
    private Listbox lbActiveTransactions;
    private Listbox lbClosedTransactions;
    private Paging pgActiveTransactions;
    private Paging pgClosedTransactions;
    private String where;

    public void onCreate() {
        initComponents();
        populateActiveTransactions(0, pgActiveTransactions.getPageSize());
        populateClosedTransactions(0, pgClosedTransactions.getPageSize());
    }

    private void initComponents() {
        lbActiveTransactions = (Listbox) getFellow("lbActiveTransactions");
        lbClosedTransactions = (Listbox) getFellow("lbClosedTransactions");
        pgActiveTransactions = (Paging) getFellow("pgActiveTransactions");
        pgClosedTransactions = (Paging) getFellow("pgClosedTransactions");

        pgActiveTransactions.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateActiveTransactions(evt.getActivePage()*pgActiveTransactions.getPageSize(), pgActiveTransactions.getPageSize());
            }
        });

        pgClosedTransactions.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateActiveTransactions(evt.getActivePage()*pgClosedTransactions.getPageSize(), pgClosedTransactions.getPageSize());
            }
        });
    }

    private void populateActiveTransactions(int offset, int limit) {
        lbActiveTransactions.getItems().clear();
        Session s = Libs.sfEDC.openSession();
        try {
//            Create policy numbers
            String policies = "";
            for (String policy : Libs.policyMap.keySet()) policies += "'" + policy.substring(policy.lastIndexOf("-")+1) + "', ";
            if (policies.endsWith(", ")) policies = policies.substring(0, policies.length()-2);

            String count = "select count(*) ";

            String select = "select "
                    + "a.transaction_id, a.no_kartu, a.tid, a.request_date, "
                    + "a.type, a.icd, a.reply_variable, c.pro_code ";

            String qry = "from edc_prj.dbo.ms_log_transaction a "
                    + "left outer join edc_prj.dbo.edc_transclm b on b.trans_id=a.transaction_id "
                    + "inner join edc_prj.dbo.edc_terminal c on c.tid=a.tid "
                    + "where substring(a.no_kartu, 6, 5) in (" + policies + ") "
                    + "and a.request_function='Validation' and a.icd<>'' and len(a.icd)>2 "
                    + "and b.trans_id is null "
                    + "and a.status='true' ";


            if (where!=null) qry += "and (" + where + ") ";

            String order = "order by request_date desc ";
            Integer recordsCount = (Integer) s.createSQLQuery(count + qry).uniqueResult();
            pgActiveTransactions.setTotalSize(recordsCount);

            List<Object[]> l = s.createSQLQuery(select + qry + order).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                String replyVariable = Libs.nn(o[6]).trim();
                String name = replyVariable.split("\\;")[5];

                EDCTransactionPOJO edcTransactionPOJO = new EDCTransactionPOJO();
                edcTransactionPOJO.setTrans_id(Libs.nn(o[0]));
                edcTransactionPOJO.setCard_number(Libs.nn(o[1]));
                edcTransactionPOJO.setType(Libs.nn(o[4]));

                Listitem li = new Listitem();
                li.setValue(edcTransactionPOJO);

                li.appendChild(new Listcell(edcTransactionPOJO.getTrans_id()));
                li.appendChild(new Listcell(edcTransactionPOJO.getCard_number()));
                li.appendChild(new Listcell(edcTransactionPOJO.getType()));
                li.appendChild(new Listcell(name));
                li.appendChild(new Listcell(Libs.nn(o[5]).trim()));
                li.appendChild(new Listcell(Libs.getICDByCode(Libs.nn(o[5]).trim())));
                li.appendChild(new Listcell(Libs.getHospitalById(Libs.nn(o[7]).trim())));
                li.appendChild(new Listcell(Libs.nn(o[3])));

                lbActiveTransactions.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void populateClosedTransactions(int offset, int limit) {
        lbClosedTransactions.getItems().clear();
        Session s = Libs.sfEDC.openSession();
        try {
//            Create policy numbers
            String policies = "";
            for (String policy : Libs.policyMap.keySet()) policies += "'" + policy.substring(policy.lastIndexOf("-")+1) + "', ";
            if (policies.endsWith(", ")) policies = policies.substring(0, policies.length()-2);

            String count = "select count(*) ";

            String select = "select "
                    + "a.trans_id, a.no_kartu, a.hclmtclaim, "
                    + "a.hclmdiscd1, a.hclmdiscd2, a.hclmdiscd3, "
                    + "a.hclmnhoscd, "
                    + "a.hclmrdatey, a.hclmrdatem, a.hclmrdated, "
                    + "(a.hclmpcode1 + a.hclmpcode2) as plan_code ";

            String qry = "from edc_prj.dbo.edc_transclm a "
                    + "where "
                    + "substring(no_kartu, 6, 5) in (" + policies + ") "
                    + "and a.hclmrecid<>'C' ";

            if (where!=null) qry += "and (" + where + ") ";

            String order = "order by a.hclmrdatey desc, a.hclmrdatem desc, a.hclmrdated desc ";
            Integer recordsCount = (Integer) s.createSQLQuery(count + qry).uniqueResult();
            pgClosedTransactions.setTotalSize(recordsCount);

            List<Object[]> l = s.createSQLQuery(select + qry + order).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                String icd = Libs.nn(o[3]).trim();
                if (!Libs.nn(o[4]).trim().isEmpty()) icd += ", " + Libs.nn(o[4]).trim();
                if (!Libs.nn(o[5]).trim().isEmpty()) icd += ", " + Libs.nn(o[5]).trim();

                EDCTransactionPOJO edcTransactionPOJO = new EDCTransactionPOJO();
                edcTransactionPOJO.setTrans_id(Libs.nn(o[0]));
                edcTransactionPOJO.setCard_number(Libs.nn(o[1]));
                edcTransactionPOJO.setType(Libs.nn(o[2]));
                edcTransactionPOJO.setIcd(icd);
                edcTransactionPOJO.setDate(Libs.nn(o[7]) + "-" + Libs.nn(o[8]) + "-" + Libs.nn(o[9]));

                Listitem li = new Listitem();
                li.setValue(edcTransactionPOJO);

                li.appendChild(new Listcell(edcTransactionPOJO.getTrans_id()));
                li.appendChild(new Listcell(edcTransactionPOJO.getCard_number()));
                li.appendChild(new Listcell(edcTransactionPOJO.getType()));
                li.appendChild(new Listcell(Libs.getMemberByCardNumber(edcTransactionPOJO.getCard_number())));
                li.appendChild(new Listcell(Libs.nn(o[10]).trim()));
                li.appendChild(new Listcell(edcTransactionPOJO.getIcd()));
                li.appendChild(new Listcell(Libs.getICDByCode(edcTransactionPOJO.getIcd())));
                li.appendChild(new Listcell(Libs.getHospitalById(Libs.nn(o[6]).trim())));
                li.appendChild(new Listcell(edcTransactionPOJO.getDate()));

                lbClosedTransactions.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

}
