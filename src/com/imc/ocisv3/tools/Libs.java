package com.imc.ocisv3.tools;

import com.imc.ocisv3.pojos.BenefitPOJO;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.Cell;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zul.Center;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Window;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by faizal on 10/24/13.
 */
public class Libs {

    private static Logger log = LoggerFactory.getLogger(Libs.class);
    public static Window rootWindow;
    public static Center center;
    public static Properties config;
    public static SessionFactory sfDB;
    public static SessionFactory sfEDC;
    public static String[] months = new String[] { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
    public static String[] shortMonths = new String[] { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
    public static String insuranceId = "00046";
    public static int userLevel = 1;
    public static Map<String,String> policyMap = new HashMap<String,String>();

    public static String nn(Object o) {
        if (o!=null) return o.toString(); else return "";
    }

    public static int getDiffDays(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(d1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(d2);

        long d1ms = cal1.getTimeInMillis();
        long d2ms = cal2.getTimeInMillis();

        return (int) (Math.ceil((d2ms-d1ms) / (float)(24*3600*1000)));
    }

    public static Listcell createNumericListcell(int d, String format) {
        Listcell lc = new Listcell(new DecimalFormat(format).format(d));
        lc.setStyle("text-align:right;");
        return lc;
    }

    public static Listcell createNumericListcell(double d, String format) {
        Listcell lc = new Listcell(new DecimalFormat(format).format(d));
        lc.setStyle("text-align:right;");
        return lc;
    }

    public static String createAddFieldString(String field) {
        String result = "";
        for (int i=0; i<30; i++) {
            result += field + (i+1) + " + ";
        }
        if (result.endsWith(" + ")) result = result.substring(0, result.length()-3);
        return result;
    }

    public static String createListFieldString(String field) {
        String result = "";
        for (int i=0; i<30; i++) {
            result += field + (i+1) + ", ";
        }
        if (result.endsWith(", ")) result = result.substring(0, result.length()-2);
        return result;
    }

    public static String getBenefitItemDescription(String planItem) {
        String result = "";
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select hrefdesc21 + hrefdesc22 "
                    + "from idnhltpf.dbo.hltref a "
                    + "where a.hrefcode1 + a.hrefcode2 = '" + planItem + "' ";

            List<String> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                result = Libs.nn(l.get(0)).trim();
            }
        } catch (Exception ex) {
            log.error("getBenefitItemDescription", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
        return result;
    }

    public static String getProposed() {
        String result = "";
        for (int i=0; i<30; i++) {
            int c = i+1;
            result += "cast(case when a.hclmref" + c + "<>'R' then a.hclmcamt" + c + "*1 else a.hclmcamt" + c + "*0 end as float) + ";
        }
        if (result.endsWith(" + ")) result = result.substring(0, result.length()-3);
        return result;
    }

    public static String getApproved() {
        String result = "";
        for (int i=0; i<30; i++) {
            int c = i+1;
            result += "cast(case when a.hclmref" + c + "<>'R' then a.hclmaamt" + c + "*1 else a.hclmaamt" + c + "*0 end as float) + ";
        }
        if (result.endsWith(" + ")) result = result.substring(0, result.length()-3);
        return result;
    }

    public static void createXLSRow(HSSFSheet sheet, int row, Object[] values) {
        HSSFRow r = sheet.createRow(row);
        int column = 0;
        for (Object o : values) {
            HSSFCell c = r.createCell(column);

            if (o instanceof String) {
                c.setCellType(Cell.CELL_TYPE_STRING);
                c.setCellValue(o.toString());
            }

            if (o instanceof Double || o instanceof Integer || o instanceof Float || o instanceof BigDecimal) {
                c.setCellType(Cell.CELL_TYPE_NUMERIC);
                c.setCellValue(Double.valueOf(o.toString()));
            }

            if (o==null) {
                c.setCellType(Cell.CELL_TYPE_BLANK);
                c.setCellValue("");
            }

            column++;
        }
    }

    public static BenefitPOJO getBenefit(String policyNumber, String planCode) {
        BenefitPOJO benefitPOJO = new BenefitPOJO();
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "a.hbftlmtamt, "
                    + Libs.createListFieldString("a.hbftbcd") + " "
                    + "from idnhltpf.dbo.hltbft a "
                    + "where "
                    + "(convert(varchar,a.hbftyy)+'-'+convert(varchar,a.hbftbr)+'-'+convert(varchar,a.hbftdist)+'-'+convert(varchar,a.hbftpono))='" + policyNumber + "' and "
                    + "ltrim(rtrim(a.hbftcode))='" + planCode + "' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);
                benefitPOJO.setPlan_code(planCode);
                benefitPOJO.setLimit(Double.valueOf(Libs.nn(o[0])));

                for (int i=0; i<30; i++) {
                    benefitPOJO.getPlan_items().add(Libs.nn(o[i+1]).trim());
                }
            }
        } catch (Exception ex) {
            log.error("getBenefit", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
        return benefitPOJO;
    }

    public static Double[] getRemainingFamilyLimit(String policyNumber, String index, String planCode) {
        Double[] result = new Double[2];

        BenefitPOJO benefitPOJO = Libs.getBenefit(policyNumber, planCode);

        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "sum( "
                    + "hclmaamt1+hclmaamt2+hclmaamt3+hclmaamt4+hclmaamt5+hclmaamt6+hclmaamt7+hclmaamt8+hclmaamt9+hclmaamt10+ "
                    + "hclmaamt11+hclmaamt12+hclmaamt13+hclmaamt14+hclmaamt15+hclmaamt16+hclmaamt17+hclmaamt18+hclmaamt19+hclmaamt20+ "
                    + "hclmaamt12+hclmaamt22+hclmaamt23+hclmaamt24+hclmaamt25+hclmaamt26+hclmaamt27+hclmaamt28+hclmaamt29+hclmaamt30 "
                    + ") "
                    + "from idnhltpf.dbo.hltclm "
                    + "where "
                    + "convert(varchar,hclmyy)+'-'+convert(varchar,hclmbr)+'-'+convert(varchar,hclmdist)+'-'+convert(varchar,hclmpono)='" + policyNumber + "' "
                    + "and hclmidxno='" + index.substring(0, index.indexOf("-")) + "' and hclmrecid<>'C' ";

            BigDecimal r = (BigDecimal) s.createSQLQuery(qry).uniqueResult();

            result[0] = (r==null) ? benefitPOJO.getLimit() : benefitPOJO.getLimit() - r.doubleValue();
            result[1] = (r==null) ? 0D : r.doubleValue();
        } catch (Exception ex) {
            log.error("getRemainingFamilyLimit", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }

        return result;
    }

    public static Double getEDCUsage(String policyNumber, String index) {
        double result = 0;
        Session s = Libs.sfEDC.openSession();
        try {
            String qry = "select "
                    + "sum(" + createAddFieldString("a.hclmaamt") + ") as approved "
                    + "from edc_prj.dbo.edc_transclm a "
                    + "where "
                    + "(convert(varchar,a.hclmyy)+'-'+convert(varchar,a.hclmbr)+'-'+convert(varchar,a.hclmdist)+'-'+convert(varchar,a.hclmpono))='" + policyNumber + "' and "
                    + "a.hclmidxno='" + index + "' ";

            BigDecimal bd = (BigDecimal) s.createSQLQuery(qry).uniqueResult();
            if (bd!=null) result = bd.doubleValue();
        } catch (Exception ex) {
            log.error("getEDCUsage", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }

        return result;
    }

    public static String getMemberByCardNumber(String cardNumber) {
        String result = "";
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "distinct a.hdt1name, a.hdt1pono "
                    + "from idnhltpf.dbo.hltdt1 a "
                    + "where a.hdt1ncard='" + cardNumber + "' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);
                result = Libs.nn(o[0]).trim();
            }
        } catch (Exception ex) {
            log.error("getMemberByCardNumber", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
        return result;
    }

    public static String getHospitalById(String id) {
        String result = "";
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "a.hproname, a.hpronomor "
                    + "from idnhltpf.dbo.hltpro a "
                    + "where a.hpronomor='" + id + "' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);
                result = Libs.nn(o[0]).trim();
            }
        } catch (Exception ex) {
            log.error("getHospitalById", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
        return result;
    }

    public static Map<String,String> getClientPlanMap(String policyNumber) {
        Map<String,String> result = new HashMap<String,String>();
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "imc_plan_code, client_plan_code "
                    + "from ocisv3.dbo.client_plan_map "
                    + "where policy_number='" + policyNumber + "' ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                result.put(Libs.nn(o[0]).trim(), Libs.nn(o[1]).trim());
            }
        } catch (Exception ex) {
            log.error("getClientPlanMap", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
        return result;
    }

    public static String getICDByCode(String icds) {
        String result = "";
        Session s = Libs.sfDB.openSession();
        try {
            String[] icdseg = icds.split("\\,");
            icds = "";
            for (String icd : icdseg) {
                icds += "'" + icd.trim() + "', ";
            }
            if (icds.endsWith(", ")) icds = icds.substring(0, icds.length()-2);

            String qry = "select "
                    + "icd_code, description "
                    + "from imcs.dbo.icds "
                    + "where icd_code in (" + icds + ") ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                result += Libs.nn(o[1]).trim() + " (" + Libs.nn(o[0]) + "), ";
            }
            if (result.endsWith(", ")) result = result.substring(0, result.length()-2);
        } catch (Exception ex) {
            log.error("getICDByCode", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }

        return result;
    }

}
