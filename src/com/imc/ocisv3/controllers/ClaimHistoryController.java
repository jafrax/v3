package com.imc.ocisv3.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;
import org.zkoss.zul.event.PagingEvent;

import com.imc.ocisv3.pojos.ClaimPOJO;
import com.imc.ocisv3.pojos.MemberPOJO;
import com.imc.ocisv3.pojos.PolicyPOJO;
import com.imc.ocisv3.tools.Libs;

/**
 * Created by faizal on 10/25/13.
 */
public class ClaimHistoryController extends Window {

    private Logger log = LoggerFactory.getLogger(ClaimHistoryController.class);
    private Listbox lb;
    private Paging pg;
    private Combobox cbPolicy;
    private Combobox cbFilter;
    private Combobox cbClaimType;
    private Textbox tQuickSearch;
    private Datebox startDate;
    private Datebox endDate;
//    private Checkbox claimVoucherCb;
    private String where;
    private String queryString;
    private String userProductViewrestriction;

    public void onCreate() {
        if (!Libs.checkSession()) {
            userProductViewrestriction = Libs.restrictUserProductView.get(Libs.getUser());
            initComponents();
            populateCountForQuickSearch();
//            populateCount();
            populate(0, pg.getPageSize());
        }
    }

    private void initComponents() {
        lb = (Listbox) getFellow("lb");
        pg = (Paging) getFellow("pg");
        cbPolicy = (Combobox) getFellow("cbPolicy");
        cbFilter = (Combobox) getFellow("cbFilter");
        cbClaimType = (Combobox)getFellow("cbClaimType");
        tQuickSearch = (Textbox)getFellow("tQuickSearch");
        startDate = (Datebox)getFellow("startDate");
        endDate = (Datebox)getFellow("endDate");
//        claimVoucherCb = (Checkbox)getFellow("claimVoucherCb");

        pg.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populate(evt.getActivePage()*pg.getPageSize(), pg.getPageSize());
            }
        });
        
        tQuickSearch.addEventListener("onOK", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
            	viewClaimHistory();
            }
        });
//        claimVoucherCb.setChecked(true);
        cbPolicy.appendItem("All Products");
        cbPolicy.setSelectedIndex(0);
        cbFilter.setSelectedIndex(0);
        cbClaimType.setSelectedIndex(0);
        
        cbClaimType.setVisible(false);
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

        Listheader lhEmployeeId = (Listheader) getFellow("lhEmployeeId");
        if (Libs.getInsuranceId().equals("00078") || Libs.getInsuranceId().equals("00088")) {
            lhEmployeeId.setVisible(true);
        } else {
            lhEmployeeId.setVisible(false);
        }
        
        endDate.setValue(new Date());
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        startDate.setValue(cal.getTime());
        
      /*  claimVoucherCb.addEventListener(Events.ON_CHECK, new EventListener<Event>() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				populate(0, pg.getPageSize());
			}
		}); */
    }
    
  
    
    public void filterBy(){
    	if(cbFilter.getSelectedIndex() == 4){
    		cbClaimType.setVisible(true);
    		tQuickSearch.setVisible(false);
    		cbClaimType.setSelectedIndex(0);
    	}else{
    		cbClaimType.setVisible(false);
    		tQuickSearch.setValue(null);
    		tQuickSearch.setVisible(true);
    	}
    }

    private void populateCount() {
        Session s = Libs.sfDB.openSession();
        try {
        	
        	String insid="";
        	List products = Libs.getProductByUserId(Libs.getUser());
        	for(int i=0; i < products.size(); i++){
        		insid=insid+"'"+(String)products.get(i)+"'"+",";
        	}
        	if(insid.length() > 1)insid = insid.substring(0, insid.length()-1);
        	
            String countSelect = "select count(*) ";

            String qry = "from idnhltpf.dbo.hlthdr b  "
                    + "inner join idnhltpf2.dbo.tclaim_header a on b.hhdryy=a.ThnPolis and b.HHDRBR=a.BrPolis and b.HHDRDIST=a.DistPolis and b.hhdrpono=a.NoPolis "
                    + "where "
                    + "b.hhdrinsid";
    				if(products.size() > 0) qry = qry + " in  ("+insid+") ";
    				else qry = qry + "='" + Libs.getInsuranceId() + "' ";  
    				
    				if (cbPolicy.getSelectedIndex()>0) {
    	                String policy = cbPolicy.getSelectedItem().getLabel();
    	                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
    	                String policyNo[] = policy.split("-");
    	                qry += "and b.hhdryy='"+policyNo[0]+"' and b.hhdrbr='"+policyNo[1]+"' and b.hhdrdist='"+policyNo[2]+"' and b.hhdrpono='" + policyNo[3] + "' ";
    	            }
    				
    				if(startDate.getValue() != null && endDate.getValue() != null){
    					qry += "and cdate BETWEEN '"+startDate.getText()+"' AND '"+endDate.getText()+"' ";
    				}

                    qry = qry + "and a.recid<>'C' AND hdt1PONO<>99999 AND hdt1IDXNO < 99989";

            if (!Libs.nn(userProductViewrestriction).isEmpty()) qry += "and b.hhdrpono in (" + userProductViewrestriction + ") ";

            if (where!=null) qry += "and (" + where + ") ";



            
//            System.out.println(countSelect + qry + "\n");

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
        	
        	String insid="";
        	List products = Libs.getProductByUserId(Libs.getUser());
        	for(int i=0; i < products.size(); i++){
        		insid=insid+"'"+(String)products.get(i)+"'"+",";
        	}
        	if(insid.length() > 1)insid = insid.substring(0, insid.length()-1);
        	
            String countSelect = "select count(*) ";

//            String qry = "from idnhltpf.dbo.hlthdr b "
//                    + "inner join idnhltpf2.dbo.tclaim_header a on b.hhdryy=a.ThnPolis and b.HHDRBR=a.BrPolis and b.HHDRDIST=a.DistPolis and b.hhdrpono=a.NoPolis "
//                    + "inner join idnhltpf.dbo.hltdt1 c  "
//                    + "on a.ThnPolis=c.hdt1yy and a.BrPolis=c.HDT1BR and a.DistPolis=c.HDT1DIST and a.NoPolis=c.hdt1pono and a.Idx=c.hdt1idxno and a.Seq=c.hdt1seqno and c.hdt1ctr=0  "
//                    + "inner join idnhltpf.dbo.hltemp e  " 
//                    + "on c.HDT1YY=e.hempyy and c.HDT1BR=e.HEMPBR and c.HDT1DIST=e.HEMPDIST and c.HDT1PONO=e.hemppono and c.HDT1IDXNO=e.hempidxno and c.HDT1SEQNO=e.hempseqno and c.HDT1CTR=e.hempctr "
//                    + "where "
//                    + "b.hhdrinsid";
            
            String qry = "from idnhltpf.dbo.hlthdr b "
                    + "inner join idnhltpf2.dbo.tclaim_header a "
                    + "on b.hhdryy=a.ThnPolis and b.HHDRBR=a.BrPolis and b.HHDRDIST=a.DistPolis and b.hhdrpono=a.NoPolis "
                    + "inner join idnhltpf.dbo.hltpro d on d.hpronomor=a.procode "
                    + "inner join idnhltpf.dbo.hltdt1 c "
                    + "on a.ThnPolis=c.hdt1yy and a.BrPolis=c.HDT1BR and a.DistPolis=c.HDT1DIST and a.NoPolis=c.hdt1pono and a.Idx=c.hdt1idxno and a.Seq=c.hdt1seqno and c.hdt1ctr=0 "
                    + "inner join idnhltpf.dbo.hltdt2 f  "
                    + "on c.HDT1YY=f.hdt2yy and c.HDT1BR=f.HDT2BR and c.HDT1DIST=f.HDT2DIST and c.HDT1PONO=f.hdt2pono and c.HDT1IDXNO=f.hdt2idxno and c.HDT1SEQNO=f.hdt2seqno and c.HDT1CTR=f.hdt2ctr "
                    + "inner join idnhltpf.dbo.hltemp e "
                    + "on f.HDT2YY=e.hempyy and f.HDT2BR=e.HEMPBR and f.HDT2DIST=e.HEMPDIST and f.HDT2PONO=e.hemppono and f.HDT2IDXNO=e.hempidxno and f.HDT2SEQNO=e.hempseqno and f.HDT2CTR=e.hempctr "
                    + "left outer join idnhltpf.dbo.hltmemo2 g  "
                    + "on a.ThnPolis=g.hmem2yy and a.BrPolis=g.HMEM2BR and a.DistPolis=g.HMEM2DIST and a.NoPolis=g.hmem2pono and a.Idx=g.hmem2idxno and a.Seq=g.hmem2seqno and a.tclaim=g.hmem2claim and a.Counter=g.hmem2count "
                    + "INNER JOIN idnhltpf.dbo.hltovc on a.hid=hovccno "
                    + "where "
                    + "b.hhdrinsid";
            
    				if(products.size() > 0) qry = qry + " in  ("+insid+") ";
    				else qry = qry + "='" + Libs.getInsuranceId() + "' ";  
    				
    				if (cbPolicy.getSelectedIndex()>0) {
    	                String policy = cbPolicy.getSelectedItem().getLabel();
    	                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
    	                String policyNo[] = policy.split("-");
    	                qry += "and b.hhdryy='"+policyNo[0]+"' and b.hhdrbr='"+policyNo[1]+"' and b.hhdrdist='"+policyNo[2]+"' and b.hhdrpono='" + policyNo[3] + "' ";
    	            }
    				
    				if(startDate.getValue() != null && endDate.getValue() != null){
    					qry += "and cdate BETWEEN '"+startDate.getText()+"' AND '"+endDate.getText()+"' ";
    				}

                    qry=qry + "and a.recid<>'C' AND hdt1PONO<>99999 AND hdt1IDXNO < 99989";

            if (!Libs.nn(userProductViewrestriction).isEmpty()) qry += "and b.hhdrpono in (" + userProductViewrestriction + ") ";

            if (where!=null) qry +=  where;

            System.out.println(countSelect + qry);

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
        	String insid="";
        	List products = Libs.getProductByUserId(Libs.getUser());
        	for(int i=0; i < products.size(); i++){
        		insid=insid+"'"+(String)products.get(i)+"'"+",";
        	}
        	if(insid.length() > 1)insid = insid.substring(0, insid.length()-1);
        	
//        	String countQry = "select count(*)  ";
        	
            String select = "select "
                    + "a.HID2 , a.ThnPolis, a.BrPolis, a.DistPolis, a.NoPolis, "
                    + "b.hhdrname, a.Idx, a.seq, "
                    + "c.hdt1name, a.tclaim, "
                    + "a.diAjukan as proposed, " //10
                    + "a.diBayarkan as approved, "
                    + "d.hproname, a.Counter, e.hempcnpol, e.hempcnid, '' as blank1, c.hdt1ncard, c.hdt1bdtyy, c.hdt1bdtmm, c.hdt1bdtdd, c.hdt1sex, " //21
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
                    + "c.hdt1mstat, " // 46
                    + "g.hmem2data1, g.hmem2data2, g.hmem2data3, g.hmem2data4, "
                    + "a.cdate, " //"a.hclmcdatey, a.hclmcdatem, a.hclmcdated, "
                    + "a.sindate, " //"a.hclmsinyy, a.hclmsinmm, a.hclmsindd, "
                    + "a.soutdate, " //"a.hclmsoutyy, a.hclmsoutmm, a.hclmsoutdd, "
                    + "a.rdate, " //"a.hclmrdatey, a.hclmrdatem, a.hclmrdated, "
                    + "a.pdate, " //"a.hclmpdatey, a.hclmpdatem, a.hclmpdated, "
                    + "a.icd1, a.icd2, a.icd3, "
                    + "a.recid, " // 59
                    + "e.hempmemo3, " // 60
            		+ "hovcoutno ";

            String qry = "from idnhltpf.dbo.hlthdr b "
                    + "inner join idnhltpf2.dbo.tclaim_header a "
                    + "on b.hhdryy=a.ThnPolis and b.HHDRBR=a.BrPolis and b.HHDRDIST=a.DistPolis and b.hhdrpono=a.NoPolis "
                    + "inner join idnhltpf.dbo.hltpro d on d.hpronomor=a.procode "
                    + "inner join idnhltpf.dbo.hltdt1 c "
                    + "on a.ThnPolis=c.hdt1yy and a.BrPolis=c.HDT1BR and a.DistPolis=c.HDT1DIST and a.NoPolis=c.hdt1pono and a.Idx=c.hdt1idxno and a.Seq=c.hdt1seqno and c.hdt1ctr=0 "
                    + "inner join idnhltpf.dbo.hltdt2 f  "
                    + "on c.HDT1YY=f.hdt2yy and c.HDT1BR=f.HDT2BR and c.HDT1DIST=f.HDT2DIST and c.HDT1PONO=f.hdt2pono and c.HDT1IDXNO=f.hdt2idxno and c.HDT1SEQNO=f.hdt2seqno and c.HDT1CTR=f.hdt2ctr "
                    + "inner join idnhltpf.dbo.hltemp e "
                    + "on f.HDT2YY=e.hempyy and f.HDT2BR=e.HEMPBR and f.HDT2DIST=e.HEMPDIST and f.HDT2PONO=e.hemppono and f.HDT2IDXNO=e.hempidxno and f.HDT2SEQNO=e.hempseqno and f.HDT2CTR=e.hempctr "
                    + "left outer join idnhltpf.dbo.hltmemo2 g  "
                    + "on a.ThnPolis=g.hmem2yy and a.BrPolis=g.HMEM2BR and a.DistPolis=g.HMEM2DIST and a.NoPolis=g.hmem2pono and a.Idx=g.hmem2idxno and a.Seq=g.hmem2seqno and a.tclaim=g.hmem2claim and a.Counter=g.hmem2count "
                    + "INNER JOIN idnhltpf.dbo.hltovc on a.hid=hovccno "
                    + "where "
                    + "b.hhdrinsid";
            
    				if(products.size() > 0) qry = qry + " in  ("+insid+") ";
    				else qry = qry + "='" + Libs.getInsuranceId() + "' ";  
    				
    				if (cbPolicy.getSelectedIndex()>0) {
    	                String policy = cbPolicy.getSelectedItem().getLabel();
    	                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
    	                String policyNo[] = policy.split("-");
    	                qry += "and b.hhdryy='"+policyNo[0]+"' and b.hhdrbr='"+policyNo[1]+"' and b.hhdrdist='"+policyNo[2]+"' and b.hhdrpono='" + policyNo[3] + "' ";
    	            }
    				
    				if(startDate.getValue() != null && endDate.getValue() != null){
    					qry += "and cdate BETWEEN '"+startDate.getText()+"' AND '"+endDate.getText()+"' ";
    				}
    				
    				

                    qry = qry + "and a.recid<>'C' AND hdt1PONO<>99999 AND hdt1IDXNO < 99989 ";

            if (!Libs.nn(userProductViewrestriction).isEmpty()) qry += "and b.hhdrpono in (" + userProductViewrestriction + ") ";

            if (where!=null) qry +=  where;

            

            //convert(date,convert(varchar,a.hclmcdated)+'-'+convert(varchar,a.hclmcdatem)+'-'+convert(varchar,a.hclmcdatey),105) desc ";
                  
            String order = "order by cdate desc ";


        	/*
            if(claimVoucherCb.isChecked()){
            	queryString = select + qry + order;
            	queryCount = countQry + select + qry;
            }
            else {
            	queryString = subqry+ select + qry + subqryend2 + order;
            	queryCount = countQry + select + qry + subqryend2;
            }*/


            
//            System.out.println("ini query ambil data history claim "+ select + qry + order);
            
//            System.out.println(select + qry + order +"\n");
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            List<Object[]> l = s.createSQLQuery(select + qry + order).setFirstResult(offset).setMaxResults(limit).list();
//            List<Object[]> l = s.createSQLQuery(queryString).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                String policyName = Libs.nn(o[5]);
                if (Libs.config.get("demo_mode").equals("true") && Libs.getInsuranceId().equals("00051")) policyName = Libs.nn(Libs.config.get("demo_name"));

                String remarks = Libs.nn(o[47]).trim();
                String provider = Libs.nn(o[12]).trim();
                if (remarks.indexOf("[")>-1 && remarks.indexOf("]")>-1) {
                    provider = remarks.substring(remarks.indexOf("[")+1, remarks.indexOf("]"));
                }

                String receiptDate = sdf.format((Date)o[54]); //Libs.nn(o[54]); //sdf.format((Date)o[55]); //o[60] + "-" + o[61] + "-" + o[62];
                String paymentDate = sdf.format((Date)o[55]); //Libs.nn(o[55]); //sdf.format((Date)o[56]); //o[63] + "-" + o[64] + "-" + o[65];
                String serviceIn = sdf.format((Date)o[52]); //Libs.nn(o[52]);//sdf.format((Date)o[53]); //o[54] + "-" + o[55] + "-" + o[56];
                String serviceOut = sdf.format((Date)o[53]); //Libs.nn(o[53]); //sdf.format((Date)o[54]); //o[57] + "-" + o[58] + "-" + o[59];

                Listitem li = new Listitem();

                Listcell cell = new Listcell();
                A hidNumber = new A(Libs.nn(o[0]));
                hidNumber.setStyle("color:#00bbee;text-decoration:none");
                cell.appendChild(hidNumber);
                li.appendChild(cell);
                
                //new filed (voucher no)
                li.appendChild(new Listcell(Libs.nn(o[61]).trim()));
                
//                li.appendChild(new Listcell(Libs.nn(o[0])));
                li.appendChild(new Listcell(Libs.nn(o[14]).trim()));
                li.appendChild(new Listcell(policyName));
                li.appendChild(new Listcell(o[6] + "-" + o[7]));
                li.appendChild(new Listcell(Libs.nn(o[15]).trim()));
                if (Libs.getInsuranceId().equals("00078") || Libs.getInsuranceId().equals("00088")) {
                    li.appendChild(new Listcell(Libs.nn(o[60]).trim()));
                } else {
                    li.appendChild(new Listcell(""));
                }
                
                cell = new Listcell();
                A memberName = new A(Libs.nn(o[8]));
                memberName.setStyle("color:#00bbee;text-decoration:none");
                cell.appendChild(memberName);
                li.appendChild(cell);
//                li.appendChild(new Listcell(Libs.nn(o[8])));
                
                /*
                 *  Author : Heri Siswanto BN
                 *  Date : 14 August 2014
                 *  Update : Change Wrap on field Status
                 */
                cell = new Listcell();
                Label lblStatus = new Label(Libs.getStatus(Libs.nn(o[59])));
                lblStatus.setPre(true);
                lblStatus.setMultiline(true);
                lblStatus.setParent(cell);
                
                li.appendChild(cell);
                li.appendChild(new Listcell(Libs.getClaimType(Libs.nn(o[9]))));
                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[10])), "#,###.##"));
                li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[11])), "#,###.##"));
                li.appendChild(new Listcell(provider));

                if (Libs.nn(o[9]).equals("I") || Libs.nn(o[9]).equals("R")) {
                    li.appendChild(new Listcell(""));
                    li.appendChild(new Listcell(serviceIn));
                    li.appendChild(new Listcell(serviceOut));
                } else {
                  li.appendChild(new Listcell(receiptDate));
                    li.appendChild(new Listcell(""));
                    li.appendChild(new Listcell(""));
                }
                
                if(paymentDate.equalsIgnoreCase("01-01-1900")) li.appendChild(new Listcell("-"));
                else li.appendChild(new Listcell(paymentDate)); 
                
                /*if (!Libs.nn(o[63]).equals("0")) {
                    li.appendChild(new Listcell(Libs.fixDate(paymentDate)));
                }*/

                lb.appendChild(li);

                PolicyPOJO policyPOJO = new PolicyPOJO();
                policyPOJO.setYear(Integer.valueOf(Libs.nn(o[1])));
                policyPOJO.setBr(Integer.valueOf(Libs.nn(o[2])));
                policyPOJO.setDist(Integer.valueOf(Libs.nn(o[3])));
                policyPOJO.setPolicy_number(Integer.valueOf(Libs.nn(o[4])));
                policyPOJO.setName(Libs.nn(o[16]).trim());

                MemberPOJO memberPOJO = new MemberPOJO();
                memberPOJO.setPolicy(policyPOJO);
                memberPOJO.setEmployee_id(Libs.nn(o[60]).trim());
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

               final  ClaimPOJO claimPOJO = new ClaimPOJO();
                claimPOJO.setPolicy(policyPOJO);
                claimPOJO.setMember(memberPOJO);
                claimPOJO.setClaim_number(Libs.nn(o[0]).trim());
                claimPOJO.setPolicy_number(Libs.nn(o[1]) + '-' + Libs.nn(o[2]) + '-' + Libs.nn(o[3]) + '-' + Libs.nn(o[4]));
                claimPOJO.setIndex(Libs.nn(o[6]) + '-' + Libs.nn(o[7]).trim());
                claimPOJO.setClaim_count(Integer.valueOf(Libs.nn(o[13])));

                li.setValue(claimPOJO);
                
                memberName.addEventListener(Events.ON_CLICK, new EventListener<Event>() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						showMemberDetail(claimPOJO);
					}
				});
                
                hidNumber.addEventListener(Events.ON_CLICK, new EventListener<Event>() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						showClaimDetail(claimPOJO);
					}
				});

//                ((Toolbarbutton) getFellow("tbnShowMemberDetail")).setDisabled(true);
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
    
    public void viewClaimHistory(){
    	if(cbFilter.getSelectedIndex() == 0){
    		//filter by Name
    		where = " AND hdt1name like '%" + tQuickSearch.getText() + "%' ";
    	}else if(cbFilter.getSelectedIndex() == 1){
    		//filter by claim no (HID)
    		where = " and hid2 like '%" + tQuickSearch.getText() + "%' ";
    	}else if(cbFilter.getSelectedIndex() == 2){
    		//filter by client policy number
    		where = " and hempcnpol like '%" + tQuickSearch.getText() + "%' "; 
    	}else if(cbFilter.getSelectedIndex() == 3){
    		//filter by product (policy name)
    		where = " and hhdrname like '%" + tQuickSearch.getText() + "%' ";
    	}else if(cbFilter.getSelectedIndex() == 4){
    		//filter by claim type
    		if(cbClaimType.getSelectedIndex() == 0)
    			where = " and tclaim='I'";
    		else where = " and tclaim<>'I'";
    	}
    	
    	/*
    	if(claimVoucherCb.isChecked()){
    		where = " and hovcoutno is not null ";
    	}
    	else {
    		where = " and hovcoutno is null ";
    	}*/
    	
    	populateCountForQuickSearch();
    	populate(0, pg.getPageSize());
    }

    public void quickSearch() {
        String val = "";// ((Textbox) getFellow("tQuickSearch")).getText();
        if (!val.isEmpty()) {
            where = "(convert(varchar,c.hdt1ncard) like '%" + val + "%' or "
                    + "c.hdt1name like '%" + val + "%' or "
                    + "e.hempcnpol like '%" + val + "%' or "
                    + "e.hempcnid like '%" + val + "%' or "
                    + "e.hempmemo3 like '%" + val + "%' or "
                    + "a.hid2 like '%" + val + "%') ";

//            populateCountForQuickSearch();
            populateCount();
            populate(0, pg.getPageSize());
        } else refresh();
    }

    public void lbSelected() {
        if (lb.getSelectedCount()>0) {
            ((Toolbarbutton) getFellow("tbnShowMemberDetail")).setDisabled(false);
        }
    }

    public void showClaimDetail(ClaimPOJO claimPojo) {
        Window w = (Window) Executions.createComponents("views/ClaimDetail.zul", this, null);
        w.setAttribute("claim", claimPojo);
        w.doModal();
    }

    public void policySelected() {
        quickSearch();
    }

    public void showMemberDetail(ClaimPOJO claimPOJO) {
    	/*if(lb.getSelectedItem() == null){
    		Messagebox.show("Select Claim History Data First!", "Information", Messagebox.OK, Messagebox.INFORMATION);
    		return;
    	}*/
//        ClaimPOJO claimPOJO = lb.getSelectedItem().getValue();
        Window w = (Window) Executions.createComponents("views/MemberDetail.zul", Libs.getRootWindow(), null);
        w.setAttribute("policy", claimPOJO.getPolicy());
        w.setAttribute("member", claimPOJO.getMember());
        w.doModal();
    }
    
    public void exportToXls(){
    	Session s = Libs.sfDB.openSession();
    	try{
    		String insid="";
        	List products = Libs.getProductByUserId(Libs.getUser());
        	for(int i=0; i < products.size(); i++){
        		insid=insid+"'"+(String)products.get(i)+"'"+",";
        	}
        	if(insid.length() > 1)insid = insid.substring(0, insid.length()-1);
        	
        	 String qry = "select "
                     + "a.HID2 , a.ThnPolis, a.BrPolis, a.DistPolis, a.NoPolis, b.hhdrname, a.Idx, a.seq, c.hdt1name, a.tclaim, "
                     + "a.diAjukan as proposed, " //10
                     + "a.diBayarkan as approved, "
                     + "d.hproname, a.Counter, e.hempcnpol, e.hempcnid, '' as blank1, c.hdt1ncard, c.hdt1bdtyy, c.hdt1bdtmm, c.hdt1bdtdd, c.hdt1sex,  " //21
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
                     + "a.cdate,"  //"a.hclmcdatey, a.hclmcdatem, a.hclmcdated, "
                     + "a.sindate, " //"a.hclmsinyy, a.hclmsinmm, a.hclmsindd, "
                     + "a.soutdate, " //"a.hclmsoutyy, a.hclmsoutmm, a.hclmsoutdd, "
                     + "a.rdate, " //"a.hclmrdatey, a.hclmrdatem, a.hclmrdated, "
                     + "a.pdate, "//"a.hclmpdatey, a.hclmpdatem, a.hclmpdated, "
                     + "a.icd1, a.icd2, a.icd3, "
                     + "a.recid, "
                     + "hovcoutno "
                     + "from idnhltpf.dbo.hlthdr b "
                     + "inner join idnhltpf2.dbo.tclaim_header a "
                     + "on b.hhdryy=a.ThnPolis and b.HHDRBR=a.BrPolis and b.HHDRDIST=a.DistPolis and b.hhdrpono=a.NoPolis "
                     + "inner join idnhltpf.dbo.hltpro d on d.hpronomor=a.procode "
                     + "inner join idnhltpf.dbo.hltdt1 c "
                     + "on a.ThnPolis=c.hdt1yy and a.BrPolis=c.HDT1BR and a.DistPolis=c.HDT1DIST and a.NoPolis=c.hdt1pono and a.Idx=c.hdt1idxno and a.Seq=c.hdt1seqno and c.hdt1ctr=0 "
                     + "inner join idnhltpf.dbo.hltdt2 f  "
                     + "on c.HDT1YY=f.hdt2yy and c.HDT1BR=f.HDT2BR and c.HDT1DIST=f.HDT2DIST and c.HDT1PONO=f.hdt2pono and c.HDT1IDXNO=f.hdt2idxno and c.HDT1SEQNO=f.hdt2seqno and c.HDT1CTR=f.hdt2ctr "
                     + "inner join idnhltpf.dbo.hltemp e "
                     + "on f.HDT2YY=e.hempyy and f.HDT2BR=e.HEMPBR and f.HDT2DIST=e.HEMPDIST and f.HDT2PONO=e.hemppono and f.HDT2IDXNO=e.hempidxno and f.HDT2SEQNO=e.hempseqno and f.HDT2CTR=e.hempctr "
                     + "left outer join idnhltpf.dbo.hltmemo2 g  "
                     + "on a.ThnPolis=g.hmem2yy and a.BrPolis=g.HMEM2BR and a.DistPolis=g.HMEM2DIST and a.NoPolis=g.hmem2pono and a.Idx=g.hmem2idxno and a.Seq=g.hmem2seqno and a.tclaim=g.hmem2claim and a.Counter=g.hmem2count "
                     + "INNER JOIN idnhltpf.dbo.hltovc on a.hid=hovccno "
                     + "where "
                     + "b.hhdrinsid";                    
     				if(products.size() > 0) qry = qry + " in  ("+insid+") ";
     				else qry = qry + "='" + Libs.getInsuranceId() + "' ";  
     				
     				if (cbPolicy.getSelectedIndex()>0) {
    	                String policy = cbPolicy.getSelectedItem().getLabel();
    	                policy = policy.substring(policy.indexOf("(")+1, policy.indexOf(")"));
    	                String policyNo[] = policy.split("-");
    	                qry += "and b.hhdryy='"+policyNo[0]+"' and b.hhdrbr='"+policyNo[1]+"' and b.hhdrdist='"+policyNo[2]+"' and b.hhdrpono='" + policyNo[3] + "' ";
    	            }
    				
    				if(startDate.getValue() != null && endDate.getValue() != null){
    					qry += "and cdate BETWEEN '"+startDate.getText()+"' AND '"+endDate.getText()+"' ";
    				}

                    qry = qry + "and a.recid<>'C' AND hdt1PONO<>99999 AND hdt1IDXNO < 99989";
                    
                    if (where!=null) qry +=  where;
                    
                    qry = qry + " order by cdate desc ";
                    
                    List<Object[]> l = s.createSQLQuery(qry).list();
                    createReport(l);
        	
        	
    	}catch(Exception ex){
    		log.error("exportToXls", ex);
    	}finally {
            if (s!=null && s.isOpen()) s.close();
        }
    	
    	
        
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
                	String insid="";
                	List products = Libs.getProductByUserId(Libs.getUser());
                	for(int i=0; i < products.size(); i++){
                		insid=insid+"'"+(String)products.get(i)+"'"+",";
                	}
                	if(insid.length() > 1)insid = insid.substring(0, insid.length()-1);
                	
                    String productName = String.valueOf(w.getAttribute("product"));
                    int period = (Integer) w.getAttribute("period");

                    String qry = "select "
                            + "a.HID2 , a.ThnPolis, a.BrPolis, a.DistPolis, a.NoPolis, b.hhdrname, a.Idx, a.seq, c.hdt1name, a.tclaim, "
                            + "a.diAjukan as proposed, " //10
                            + "a.diBayarkan as approved, "
                            + "d.hproname, a.Counter, e.hempcnpol, e.hempcnid, '' as blank1, c.hdt1ncard, c.hdt1bdtyy, c.hdt1bdtmm, c.hdt1bdtdd, c.hdt1sex,  " //21
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
                            + "a.cdate,"  //"a.hclmcdatey, a.hclmcdatem, a.hclmcdated, "
                            + "a.sindate, " //"a.hclmsinyy, a.hclmsinmm, a.hclmsindd, "
                            + "a.soutdate, " //"a.hclmsoutyy, a.hclmsoutmm, a.hclmsoutdd, "
                            + "a.rdate, " //"a.hclmrdatey, a.hclmrdatem, a.hclmrdated, "
                            + "a.pdate, "//"a.hclmpdatey, a.hclmpdatem, a.hclmpdated, "
                            + "a.icd1, a.icd2, a.icd3, "
                            + "a.recid "
                            + "from idnhltpf.dbo.hlthdr b "
                            + "inner join idnhltpf2.dbo.tclaim_header a "
                            + "on b.hhdryy=a.ThnPolis and b.HHDRBR=a.BrPolis and b.HHDRDIST=a.DistPolis and b.hhdrpono=a.NoPolis "
                            + "inner join idnhltpf.dbo.hltpro d on d.hpronomor=a.procode "
                            + "inner join idnhltpf.dbo.hltdt1 c "
                            + "on a.ThnPolis=c.hdt1yy and a.BrPolis=c.HDT1BR and a.DistPolis=c.HDT1DIST and a.NoPolis=c.hdt1pono and a.Idx=c.hdt1idxno and a.Seq=c.hdt1seqno and c.hdt1ctr=0 "
                            + "inner join idnhltpf.dbo.hltdt2 f  "
                            + "on c.HDT1YY=f.hdt2yy and c.HDT1BR=f.HDT2BR and c.HDT1DIST=f.HDT2DIST and c.HDT1PONO=f.hdt2pono and c.HDT1IDXNO=f.hdt2idxno and c.HDT1SEQNO=f.hdt2seqno and c.HDT1CTR=f.hdt2ctr "
                            + "inner join idnhltpf.dbo.hltemp e "
                            + "on f.HDT2YY=e.hempyy and f.HDT2BR=e.HEMPBR and f.HDT2DIST=e.HEMPDIST and f.HDT2PONO=e.hemppono and f.HDT2IDXNO=e.hempidxno and f.HDT2SEQNO=e.hempseqno and f.HDT2CTR=e.hempctr "
                            + "left outer join idnhltpf.dbo.hltmemo2 g  "
                            + "on a.ThnPolis=g.hmem2yy and a.BrPolis=g.HMEM2BR and a.DistPolis=g.HMEM2DIST and a.NoPolis=g.hmem2pono and a.Idx=g.hmem2idxno and a.Seq=g.hmem2seqno and a.tclaim=g.hmem2claim and a.Counter=g.hmem2count "
                            + "where "
                            + "b.hhdrinsid";                    
            				if(products.size() > 0) qry = qry + " in  ("+insid+") ";
            				else qry = qry + "='" + Libs.getInsuranceId() + "' ";  

                            qry = qry + "and a.recid<>'C' AND hdt1PONO<>99999 AND hdt1IDXNO < 99989";

                    if (!Libs.nn(userProductViewrestriction).isEmpty()) qry += "and b.hhdrpono in (" + userProductViewrestriction + ") ";

                    if (!productName.toLowerCase().equals("all products")) {
                        String policy = productName.substring(productName.indexOf("(")+1, productName.indexOf(")"));
                        String policyNo[] = policy.split("-");
    	                qry += "and b.hhdryy='"+policyNo[0]+"' and b.hhdrbr='"+policyNo[1]+"' and b.hhdrdist='"+policyNo[2]+"' and b.hhdrpono='" + policyNo[3] + "' ";
    	                
//                        qry += "and (convert(varchar,a.hclmyy)+'-'+convert(varchar,a.hclmbr)+'-'+convert(varchar,a.hclmdist)+'-'+convert(varchar,a.hclmpono)='" + policy + "') ";
                    }
                    
                    Date dateStart = (Date) w.getAttribute("dateStart");
                    Date dateEnd = (Date) w.getAttribute("dateEnd");
                    
                    qry += "and a.cdate between '" + new SimpleDateFormat("yyyy-MM-dd").format(dateStart) + "' and '" + new SimpleDateFormat("yyyy-MM-dd").format(dateEnd) + "'";
                    
                    /*

                    switch (period) {
                        case 0:
                            Date date = (Date) w.getAttribute("date");
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(date);
                            cal.set(Calendar.DAY_OF_MONTH, 1);
                            cal.set(Calendar.MONTH, 0);
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
                            int maxDay = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
                            Calendar calStart = Calendar.getInstance();
                            Calendar calEnd = Calendar.getInstance();
                            calStart.set(monthYearStart, monthStart, 1);
                            calEnd.set(monthYearEnd, monthEnd, maxDay);
                            qry += "and convert(datetime,convert(varchar,hclmcdated)+'-'+convert(varchar,hclmcdatem)+'-'+convert(varchar,hclmcdatey),105) between '" + new SimpleDateFormat("yyyy-MM-dd").format(calStart.getTime()) + "' and '" + new SimpleDateFormat("yyyy-MM-dd").format(calEnd.getTime()) + "'";
                            break;
                        case 4:
                            int yearStart = (Integer) w.getAttribute("yearStart");
                            int yearEnd = (Integer) w.getAttribute("yearEnd");
                            Calendar calStart1 = Calendar.getInstance();
                            Calendar calEnd1 = Calendar.getInstance();
                            calStart1.set(yearStart, 0, 1);
                            calEnd1.set(yearEnd, 11, 31);
                            qry += "and convert(datetime,convert(varchar,hclmcdated)+'-'+convert(varchar,hclmcdatem)+'-'+convert(varchar,hclmcdatey),105) between '" + new SimpleDateFormat("yyyy-MM-dd").format(calStart1.getTime()) + "' and '" + new SimpleDateFormat("yyyy-MM-dd").format(calEnd1.getTime()) + "'";
                            break;
                    }
                    */
                    
                    System.out.println("\n" +qry);

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
        /*String[] columnsMemberWise = new String[] {
                "POLICY YEAR", "BR", "DIST", "POLICY NUMBER", "COMPANY NAME", "INDEX", "SEQ", "CARD NUMBER",
                "NAME", "COUNT", "TYPE", "CLAIM-YEAR", "CLAIM-MONTH", "CLAIM-DAY", "SIN-YEAR", "SIN-MONTH",
                "SIN-DAY", "SOUT-YEAR", "SOUT-MONTH", "SOUT-DAY", "RECEIPT-YEAR", "RECEIPT-MONTH",
                "RECEIPT-DAY", "PAYMENT-YEAR", "PAYMENT-MONTH", "PAYMENT-DAY", "HID NUMBER", "PROVIDER NAME",
                "ICD1", "ICD2", "ICD3", "PROPOSED", "APPROVED", "STATUS", "MEMO" };*/
    	
    	String[] columnsMemberWise = new String[] {
                "POLICY YEAR", "BR", "DIST", "POLICY NUMBER", "COMPANY NAME", "INDEX", "SEQ", "CARD NUMBER",
                "NAME", "COUNT", "TYPE", "CLAIM-DATE", "SIN-DATE", "SOUT-DATE", "RECEIPT-DATE", "PAYMENT-DATE", 
                "HID NUMBER", "VOUCHER NO", "PROVIDER NAME", "ICD1", "ICD2", "ICD3", "PROPOSED", "APPROVED", "STATUS", "MEMO" };
    	
    	SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        try {
            Workbook wb = new HSSFWorkbook();
            Sheet sheet = wb.createSheet("Claim History");
            
            int cnt = 0;
            
          
            StringBuffer sb = new StringBuffer();
            String judul = "Claim History Periode "+sdf.format(startDate.getValue()) + " To "+sdf.format(endDate.getValue());
            
            sb.append(judul);
            if(cbFilter.getSelectedIndex() != 4 && !tQuickSearch.getText().equalsIgnoreCase("")){
            	sb.append(" Filter By " +cbFilter.getSelectedItem().getLabel() + " : "+ tQuickSearch.getText());
            }else if(cbFilter.getSelectedIndex() == 4){
            	sb.append(" Filter By " +cbFilter.getSelectedItem().getLabel() + " : "+ cbClaimType.getSelectedItem().getLabel());
            }
            
            sb.append(" Product : " + cbPolicy.getSelectedItem().getLabel());
            
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(cnt);
            Libs.createCell(row, 0, sb.toString());
            
            cnt++;
            
            row = sheet.createRow(cnt);
            
            cnt++;
            row = sheet.createRow(cnt);
            
            cnt++;
            row = sheet.createRow(cnt);

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
                
                String receiptDate = sdf.format((Date)o[54]); if(receiptDate.equalsIgnoreCase("01-01-1900")) receiptDate="-";
                String paymentDate = sdf.format((Date)o[55]); if(paymentDate.equalsIgnoreCase("01-01-1900")) paymentDate="-";
                String serviceIn = sdf.format((Date)o[52]); if(serviceIn.equalsIgnoreCase("01-01-1900")) serviceIn="-";
                String serviceOut = sdf.format((Date)o[53]); if(serviceOut.equalsIgnoreCase("01-01-1900")) serviceOut="-";
                String claimDate = sdf.format((Date)o[51]);
                
                Libs.createCell(row, 11, claimDate);  //Libs.createCell(row, 11, Libs.nn(o[51]));
                Libs.createCell(row, 12, serviceIn);  //Libs.createCell(row, 12, Libs.nn(o[52]));
                Libs.createCell(row, 13, serviceOut);  //Libs.createCell(row, 13, Libs.nn(o[53]));
                Libs.createCell(row, 14, receiptDate); //Libs.createCell(row, 14, Libs.nn(o[54]));
                Libs.createCell(row, 15, paymentDate);  //Libs.createCell(row, 15, Libs.nn(o[55]));
                
                /*
                Libs.createCell(row, 16, Libs.nn(o[56]));
                Libs.createCell(row, 17, Libs.nn(o[57]));
                Libs.createCell(row, 18, Libs.nn(o[58]));
                Libs.createCell(row, 19, Libs.nn(o[59]));
                Libs.createCell(row, 20, Libs.nn(o[60]));
                Libs.createCell(row, 21, Libs.nn(o[61]));
                Libs.createCell(row, 22, Libs.nn(o[62]));
                Libs.createCell(row, 23, Libs.nn(o[63]));
                Libs.createCell(row, 24, Libs.nn(o[64]));
                Libs.createCell(row, 25, Libs.nn(o[65]));*/
                
                Libs.createCell(row, 16, Libs.nn(o[0]));
                Libs.createCell(row, 17, Libs.nn(o[60]));
                
                Libs.createCell(row, 18, Libs.nn(o[12]));
                Libs.createCell(row, 19, Libs.nn(o[56]));
                Libs.createCell(row, 20, Libs.nn(o[57]));
                Libs.createCell(row, 21, Libs.nn(o[58]));
                
                BigDecimal d =(BigDecimal)o[10];
                Cell cell = row.createCell(22); //Libs.createCell(row, 22, o[10]);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                cell.setCellValue(d.doubleValue());
                
                d = (BigDecimal)o[11];
                cell = row.createCell(23); //Libs.createCell(row, 22, o[10]);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                cell.setCellValue(d.doubleValue());
                
                
//                Libs.createCell(row, 23, o[11]);
                Libs.createCell(row, 24, Libs.getStatus(Libs.nn(o[59])));
                Libs.createCell(row, 25, Libs.nn(o[47]).trim() + Libs.nn(o[48]).trim() + Libs.nn(o[49]).trim() + Libs.nn(o[50]).trim());

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
