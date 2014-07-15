package com.imc.ocisv3.controllers;

import com.imc.ocisv3.pojos.PolicyPOJO;
import com.imc.ocisv3.tools.Libs;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.Type;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by faizal on 10/24/13.
 */
public class PoliciesController extends Window {

    private Logger log = LoggerFactory.getLogger(PoliciesController.class);
    private Listbox lb;
    private Paging pg;
    private String where;
    private String userProductViewrestriction;

    public void onCreate() {
        if (!Libs.checkSession()) {
            userProductViewrestriction = Libs.restrictUserProductView.get(Libs.getUser());
            initComponents();
            populate(0, pg.getPageSize());
        }
    }

    private void initComponents() {
        lb = (Listbox) getFellow("lb");
        pg = (Paging) getFellow("pg");

        pg.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populate(evt.getActivePage()*pg.getPageSize(), pg.getPageSize());
            }
        });
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
        	
            String countQry = "select count(*) "
                    + "from idnhltpf.dbo.hlthdr a "
                    + "where "
                    + "a.hhdrinsid";
            
            
                    if(products.size() > 0){
                    	countQry = countQry + " in ("+insid+")";
                    }
                    else{
                    	countQry = countQry + " = '" + Libs.getInsuranceId() + "'";
                    }
                  

            String qry = "select "
                    + "a.hhdryy, a.hhdrbr, a.hhdrdist, a.hhdrpono, "
                    + "a.hhdrname, "
                    + "a.hhdrsdtyy, a.hhdrsdtmm, a.hhdrsdtdd, "
                    + "a.hhdrmdtyy, a.hhdrmdtmm, a.hhdrmdtdd "
                    + "from idnhltpf.dbo.hlthdr a "
                    + "where "
                    + "a.hhdrinsid";
            	if(products.size() > 0) qry = qry + " in  ("+insid+")";
            	else qry = qry + " = '" + Libs.getInsuranceId() + "'";
            
            

            if (where!=null) {
                countQry += "and (" + where + ") ";
                qry += "and (" + where + ") ";
            }

            SQLQuery query = s.createSQLQuery(countQry);
                      
            Integer count = (Integer) query.uniqueResult();
            pg.setTotalSize(count);
            
            query = s.createSQLQuery(qry);
           
  
            boolean show = true;
//            List<Object[]> l = s.createSQLQuery(qry).setFirstResult(offset).setMaxResults(limit).list();
            List<Object[]> l = query.setFirstResult(offset).setMaxResults(limit).list();
            
            for (Object[] o : l) {
                String policyNumber = Libs.nn(o[0]) + "-" + Libs.nn(o[1]) + "-" + Libs.nn(o[2]) + "-" + Libs.nn(o[3]);
                String startingDate = Libs.nn(o[5]) + "-" + Libs.nn(o[6]) + "-" + Libs.nn(o[7]);
                String matureDate = Libs.nn(o[8]) + "-" + Libs.nn(o[9]) + "-" + Libs.nn(o[10]);
                int matureDays = Libs.getDiffDays(new Date(), new SimpleDateFormat("yyyy-MM-dd").parse(matureDate));

                Listcell lcStatus = new Listcell();
                Label lStatus = new Label();
                if (matureDays>0) {
                    lStatus.setValue("ACTIVE");
                    lStatus.setStyle("color:#00FF00");
                } else {
                    lStatus.setValue("MATURE");
                    lStatus.setStyle("color:#FF0000;");
                }
                lcStatus.appendChild(lStatus);

                Listitem li = new Listitem();

                PolicyPOJO policyPOJO = new PolicyPOJO();
                policyPOJO.setYear(Integer.valueOf(Libs.nn(o[0])));
                policyPOJO.setBr(Integer.valueOf(Libs.nn(o[1])));
                policyPOJO.setDist(Integer.valueOf(Libs.nn(o[2])));
                policyPOJO.setPolicy_number(Integer.valueOf(Libs.nn(o[3])));
                policyPOJO.setName(Libs.nn(o[4]).trim());

                li.setValue(policyPOJO);

                li.appendChild(new Listcell(policyNumber));
                li.appendChild(new Listcell(((Libs.config.get("demo_mode")).equals("true") && Libs.getInsuranceId().equals("00051")) ? Libs.nn(Libs.config.get("demo_name")) : Libs.nn(o[4]).trim()));
                li.appendChild(lcStatus);
                li.appendChild(Libs.createNumericListcell(getMemberCount(Libs.nn(o[0]), Libs.nn(o[3])), "#,###"));
                li.appendChild(new Listcell(startingDate));
                li.appendChild(new Listcell(matureDate));

                if (!Libs.nn(userProductViewrestriction).isEmpty()) {
                    if (!userProductViewrestriction.contains(Libs.nn(o[3]))) show=false;
                    else show=true;
                }

                if (show) lb.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
            
        }
    }

    private int getMemberCount(String hdt1yy, String hdt1pono) {
        int result = 0;
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select count(*) "
                    + "from idnhltpf.dbo.hltdt1 a "
                    + "where "
                    + "a.hdt1yy=" + hdt1yy + " and a.hdt1pono=" + hdt1pono;

            result = (Integer) s.createSQLQuery(qry).uniqueResult();
        } catch (Exception ex) {
            log.error("getMemberCount", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
        return result;
    }

    public void refresh() {
        where = null;
        populate(0, pg.getPageSize());
    }

    public void quickSearch() {
        String val = ((Textbox) getFellow("tQuickSearch")).getText();
        if (!val.isEmpty()) {
            where = "convert(varchar,a.hhdrpono) like '%" + val + "%' or "
                    + "a.hhdrname like '%" + val + "%' ";
            populate(0, pg.getPageSize());
        } else refresh();
    }

    public void lbSelected() {

    }

    public void showPolicyDetail() {
        if (Libs.getCenter().getChildren().size()>0) Libs.getCenter().removeChild(Libs.getCenter().getFirstChild());
        Window w = (Window) Executions.createComponents("views/PolicyDetail.zul", Libs.getCenter(), null);
        w.setAttribute("policy", lb.getSelectedItem().getValue());
    }

    public void export() {
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "a.hhdryy, a.hhdrbr, a.hhdrdist, a.hhdrpono, "
                    + "a.hhdrname, "
                    + "a.hhdrsdtyy, a.hhdrsdtmm, a.hhdrsdtdd, "
                    + "a.hhdrmdtyy, a.hhdrmdtmm, a.hhdrmdtdd "
                    + "from idnhltpf.dbo.hlthdr a "
                    + "where "
                    + "a.hhdrinsid='" + Libs.getInsuranceId() + "' ";

            if (where!=null) qry += "and (" + where + ") ";

            String uuid = UUID.randomUUID().toString();
            File f = new File(Libs.config.get("temp_dir").toString() + File.separator + uuid);
            FileOutputStream fos = new FileOutputStream(f);

            HSSFWorkbook wb = new HSSFWorkbook();
            HSSFSheet sheet = wb.createSheet("Policies");
            Libs.createXLSRow(sheet, 0, new Object[] {
                    "Policy Number",
                    "Name",
                    "Status",
                    "Members",
                    "Starting Date",
                    "Mature Date",
            });

            int row = 1;
            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                String policyNumber = Libs.nn(o[0]) + "-" + Libs.nn(o[1]) + "-" + Libs.nn(o[2]) + "-" + Libs.nn(o[3]);
                String startingDate = Libs.nn(o[5]) + "-" + Libs.nn(o[6]) + "-" + Libs.nn(o[7]);
                String matureDate = Libs.nn(o[8]) + "-" + Libs.nn(o[9]) + "-" + Libs.nn(o[10]);
                int matureDays = Libs.getDiffDays(new Date(), new SimpleDateFormat("yyyy-MM-dd").parse(matureDate));

                String status = (matureDays>0) ? "ACTIVE" : "MATURE";

                Libs.createXLSRow(sheet, row, new Object[] {
                        policyNumber,
                        Libs.nn(o[4]).trim(),
                        status,
                        getMemberCount(Libs.nn(o[0]), Libs.nn(o[3])),
                        startingDate,
                        matureDate
                });
                row++;
            }

            wb.write(fos);
            fos.close();

            FileInputStream fis = new FileInputStream(f);
            Filedownload.save(fis, "Application/Excel", Libs.getInsuranceId() + "-Policies-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xls");
        } catch (Exception ex) {
            log.error("export", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

}
