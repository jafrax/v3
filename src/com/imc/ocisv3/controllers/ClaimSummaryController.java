package com.imc.ocisv3.controllers;

import com.imc.ocisv3.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zul.*;

import java.text.DecimalFormat;
import java.util.*;
import java.util.Calendar;

/**
 * Created by faizal on 10/25/13.
 */
public class ClaimSummaryController extends Window {

    private Logger log = LoggerFactory.getLogger(ClaimSummaryController.class);
    private Listbox lbFrequency;
    private Listbox lbAmount;
    private Listbox lbHID;
    private Listbox lbExcessClaimAmount;
    private Flashchart chartFrequency;
    private Flashchart chartAmount;
    private Flashchart chartHID;
    private Combobox cbPolicy;

    public void onCreate() {
        initComponents();
        populateFrequency();
        populateAmount();
        populateHID();
        populateExcessClaimAmount();
    }

    private void initComponents() {
        lbFrequency = (Listbox) getFellow("lbFrequency");
        lbAmount = (Listbox) getFellow("lbAmount");
        lbHID = (Listbox) getFellow("lbHID");
        lbExcessClaimAmount = (Listbox) getFellow("lbExcessClaimAmount");
        chartFrequency = (Flashchart) getFellow("chartFrequency");
        chartAmount = (Flashchart) getFellow("chartAmount");
        chartHID = (Flashchart) getFellow("chartHID");
        cbPolicy = (Combobox) getFellow("cbPolicy");

        Listhead lhFrequency = lbFrequency.getListhead();
        Listhead lhAmount = lbAmount.getListhead();
        Listhead lhHID = lbHID.getListhead();
        Listhead lhExcessClaimAmount = lbExcessClaimAmount.getListhead();

        for (int i=-11; i<1; i++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, i);
            Listheader lhdr = new Listheader(Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + cal.get(Calendar.YEAR));
            lhdr.setWidth("70px");
            lhFrequency.appendChild(lhdr);
        }

        for (int i=-11; i<1; i++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, i);
            Listheader lhdr = new Listheader(Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + cal.get(Calendar.YEAR));
            lhdr.setWidth("100px");
            lhAmount.appendChild(lhdr);
        }

        for (int i=-11; i<1; i++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, i);
            Listheader lhdr = new Listheader(Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + cal.get(Calendar.YEAR));
            lhdr.setWidth("70px");
            lhHID.appendChild(lhdr);
        }

        for (int i=-11; i<1; i++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, i);
            Listheader lhdr = new Listheader(Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + cal.get(Calendar.YEAR));
            lhdr.setWidth("70px");
            lhExcessClaimAmount.appendChild(lhdr);
        }

        cbPolicy.appendItem("All Policies");
        cbPolicy.setSelectedIndex(0);
        for (String s : Libs.policyMap.keySet()) {
            String policyName = Libs.policyMap.get(s);
            if (Libs.config.get("demo_mode").equals("true") && Libs.insuranceId.equals("00051")) policyName = "HAS - P.T. Semesta Alam";

            cbPolicy.appendItem(policyName + " (" + s + ")");
        }
    }

    private void populateFrequency() {
        lbFrequency.getItems().clear();
        Map<String,Integer> claimMap = new HashMap<String,Integer>();

        CategoryModel cm = new SimpleCategoryModel();

        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "a.hclmcdatey, a.hclmcdatem, a.hclmtclaim, count(*) "
                    + "from idnhltpf.dbo.hltclm a "
                    + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hclmyy and b.hhdrpono=a.hclmpono "
                    + "where "
                    + "b.hhdrinsid='" + Libs.insuranceId + "' and "
                    + "a.hclmrecid<>'C' ";

            if (cbPolicy.getSelectedIndex()>0) {
                String policy = cbPolicy.getSelectedItem().getLabel();
                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
                qry += "and (convert(varchar,a.hclmyy)+'-'+convert(varchar,a.hclmbr)+'-'+convert(varchar,a.hclmdist)+'-'+convert(varchar,a.hclmpono)='" + policy + "') ";
            }

            qry += "group by a.hclmcdatey, a.hclmcdatem, a.hclmtclaim ";

            List<Object[]> l = s.createSQLQuery(qry).list();

            for (Object[] o : l) {
                String key = o[0] + "-" + (Integer.valueOf(Libs.nn(o[1]))-1) + "-" + o[2];
                claimMap.put(key, Integer.valueOf(Libs.nn(o[3])));
            }

            int emptyInpatient = 0;
            Listitem liInpatient = new Listitem();
            liInpatient.appendChild(new Listcell("INPATIENT"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-I";
                if (claimMap.get(key)==null) emptyInpatient++;
                liInpatient.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Inpatient", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            int emptyOutpatient = 0;
            Listitem liOutpatient = new Listitem();
            liOutpatient.appendChild(new Listcell("OUTPATIENT"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-O";
                if (claimMap.get(key)==null) emptyOutpatient++;
                liOutpatient.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Outpatient", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            int emptyMaternity = 0;
            Listitem liMaternity = new Listitem();
            liMaternity.appendChild(new Listcell("MATERNITY"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-R";
                if (claimMap.get(key)==null) emptyMaternity++;
                liMaternity.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Maternity", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            int emptyDental = 0;
            Listitem liDental = new Listitem();
            liDental.appendChild(new Listcell("DENTAL"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-D";
                if (claimMap.get(key)==null) emptyDental++;
                liDental.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Dental", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            int emptyGlasses = 0;
            Listitem liGlasses = new Listitem();
            liGlasses.appendChild(new Listcell("GLASSES"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-G";
                if (claimMap.get(key)==null) emptyGlasses++;
                liGlasses.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Glasses", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            if (liInpatient.getChildren().size()>1 && emptyInpatient!=12) lbFrequency.appendChild(liInpatient);
            if (liOutpatient.getChildren().size()>1 && emptyOutpatient!=12) lbFrequency.appendChild(liOutpatient);
            if (liMaternity.getChildren().size()>1 && emptyMaternity!=12) lbFrequency.appendChild(liMaternity);
            if (liDental.getChildren().size()>1 && emptyDental!=12) lbFrequency.appendChild(liDental);
            if (liGlasses.getChildren().size()>1 && emptyGlasses!=12) lbFrequency.appendChild(liGlasses);

            chartFrequency.setModel(cm);
        } catch (Exception ex) {
            log.error("populateFrequency", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void populateAmount() {
        lbAmount.getItems().clear();
        Map<String,Double> claimMap = new HashMap<String,Double>();

        CategoryModel cm = new SimpleCategoryModel();

        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "a.hclmcdatey, a.hclmcdatem, a.hclmtclaim, "
                    + "sum(" + Libs.createAddFieldString("a.hclmaamt") + ") as approved "
                    + "from idnhltpf.dbo.hltclm a "
                    + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hclmyy and b.hhdrpono=a.hclmpono "
                    + "where "
                    + "b.hhdrinsid='" + Libs.insuranceId + "' and "
                    + "a.hclmrecid<>'C' ";

            if (cbPolicy.getSelectedIndex()>0) {
                String policy = cbPolicy.getSelectedItem().getLabel();
                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
                qry += "and (convert(varchar,a.hclmyy)+'-'+convert(varchar,a.hclmbr)+'-'+convert(varchar,a.hclmdist)+'-'+convert(varchar,a.hclmpono)='" + policy + "') ";
            }

            qry += "group by a.hclmcdatey, a.hclmcdatem, a.hclmtclaim ";

            List<Object[]> l = s.createSQLQuery(qry).list();

            for (Object[] o : l) {
                String key = o[0] + "-" + (Integer.valueOf(Libs.nn(o[1]))-1) + "-" + o[2];
                claimMap.put(key, Double.valueOf(Libs.nn(o[3])));
            }

            int emptyInpatient = 0;
            Listitem liInpatient = new Listitem();
            liInpatient.appendChild(new Listcell("INPATIENT"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-I";
                if (claimMap.get(key)==null) emptyInpatient++;
                liInpatient.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Inpatient", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            int emptyOutpatient = 0;
            Listitem liOutpatient = new Listitem();
            liOutpatient.appendChild(new Listcell("OUTPATIENT"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-O";
                if (claimMap.get(key)==null) emptyOutpatient++;
                liOutpatient.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Outpatient", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            int emptyMaternity = 0;
            Listitem liMaternity = new Listitem();
            liMaternity.appendChild(new Listcell("MATERNITY"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-R";
                if (claimMap.get(key)==null) emptyMaternity++;
                liMaternity.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Maternity", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            int emptyDental = 0;
            Listitem liDental = new Listitem();
            liDental.appendChild(new Listcell("DENTAL"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-D";
                if (claimMap.get(key)==null) emptyDental++;
                liDental.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Dental", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            int emptyGlasses = 0;
            Listitem liGlasses = new Listitem();
            liGlasses.appendChild(new Listcell("GLASSES"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-G";
                if (claimMap.get(key)==null) emptyGlasses++;
                liGlasses.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Glasses", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            if (liInpatient.getChildren().size()>1 && emptyInpatient!=12) lbAmount.appendChild(liInpatient);
            if (liOutpatient.getChildren().size()>1 && emptyOutpatient!=12) lbAmount.appendChild(liOutpatient);
            if (liMaternity.getChildren().size()>1 && emptyMaternity!=12) lbAmount.appendChild(liMaternity);
            if (liDental.getChildren().size()>1 && emptyDental!=12) lbAmount.appendChild(liDental);
            if (liGlasses.getChildren().size()>1 && emptyGlasses!=12) lbAmount.appendChild(liGlasses);

            chartAmount.setModel(cm);
        } catch (Exception ex) {
            log.error("populateAmount", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void populateHID() {
        lbHID.getItems().clear();
        Map<String,Double> claimMap = new HashMap<String,Double>();

        CategoryModel cm = new SimpleCategoryModel();

        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "a.hclmcdatey, a.hclmcdatem, a.hclmtclaim, count(distinct a.hclmcno) "
                    + "from idnhltpf.dbo.hltclm a "
                    + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.hclmyy and b.hhdrpono=a.hclmpono "
                    + "where "
                    + "b.hhdrinsid='" + Libs.insuranceId + "' and hclmrecid<>'C' ";

            if (cbPolicy.getSelectedIndex()>0) {
                String policy = cbPolicy.getSelectedItem().getLabel();
                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
                qry += "and (convert(varchar,a.hclmyy)+'-'+convert(varchar,a.hclmbr)+'-'+convert(varchar,a.hclmdist)+'-'+convert(varchar,a.hclmpono)='" + policy + "') ";
            }

            qry += "group by a.hclmcdatey, a.hclmcdatem, a.hclmtclaim ";

            List<Object[]> l = s.createSQLQuery(qry).list();

            for (Object[] o : l) {
                String key = o[0] + "-" + (Integer.valueOf(Libs.nn(o[1]))-1) + "-" + o[2];
                claimMap.put(key, Double.valueOf(Libs.nn(o[3])));
            }

            int emptyInpatient = 0;
            Listitem liInpatient = new Listitem();
            liInpatient.appendChild(new Listcell("INPATIENT"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-I";
                if (claimMap.get(key)==null) emptyInpatient++;
                liInpatient.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Inpatient", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            int emptyOutpatient = 0;
            Listitem liOutpatient = new Listitem();
            liOutpatient.appendChild(new Listcell("OUTPATIENT"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-O";
                if (claimMap.get(key)==null) emptyOutpatient++;
                liOutpatient.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Outpatient", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            int emptyMaternity = 0;
            Listitem liMaternity = new Listitem();
            liMaternity.appendChild(new Listcell("MATERNITY"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-R";
                if (claimMap.get(key)==null) emptyMaternity++;
                liMaternity.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Maternity", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            int emptyDental = 0;
            Listitem liDental = new Listitem();
            liDental.appendChild(new Listcell("DENTAL"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-D";
                if (claimMap.get(key)==null) emptyDental++;
                liDental.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Dental", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            int emptyGlasses = 0;
            Listitem liGlasses = new Listitem();
            liGlasses.appendChild(new Listcell("GLASSES"));
            for (int i=-11; i<1; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, i);
                String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-G";
                if (claimMap.get(key)==null) emptyGlasses++;
                liGlasses.appendChild((claimMap.get(key))==null ? new Listcell("") : new ClaimListcell(this, key, claimMap.get(key), "#"));
                cm.setValue("Glasses", Libs.shortMonths[cal.get(Calendar.MONTH)] + " " + String.valueOf(cal.get(Calendar.YEAR)).substring(2), claimMap.get(key));
            }

            if (liInpatient.getChildren().size()>1 && emptyInpatient!=12) lbHID.appendChild(liInpatient);
            if (liOutpatient.getChildren().size()>1 && emptyOutpatient!=12) lbHID.appendChild(liOutpatient);
            if (liMaternity.getChildren().size()>1 && emptyMaternity!=12) lbHID.appendChild(liMaternity);
            if (liDental.getChildren().size()>1 && emptyDental!=12) lbHID.appendChild(liDental);
            if (liGlasses.getChildren().size()>1 && emptyGlasses!=12) lbHID.appendChild(liGlasses);

            chartHID.setModel(cm);
        } catch (Exception ex) {
            log.error("populateHID", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void populateExcessClaimAmount() {

    }

    class ClaimListcell extends Listcell {

        public ClaimListcell(final Window w, final String key, final double value, final String format) {
            setLabel(new DecimalFormat("#,###.##").format(value));
            setStyle("text-align:right;");
            addEventListener("onDoubleClick", new org.zkoss.zk.ui.event.EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    String policy = "";

                    if (cbPolicy.getSelectedIndex()>0) {
                        policy = cbPolicy.getSelectedItem().getLabel();
                        policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
                    }

                    Window wl = (Window) Executions.createComponents("views/ClaimList.zul", w, null);
                    wl.setAttribute("policy", policy);
                    wl.setAttribute("key", key);
                    wl.doModal();
                }
            });
        }

    }

    public void policySelected() {
        populateFrequency();
        populateAmount();
        populateHID();
        populateExcessClaimAmount();
    }

}