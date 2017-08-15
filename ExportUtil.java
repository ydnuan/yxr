package cn.com.maxtech.tools.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jxl.Cell;
import jxl.CellType;
import jxl.FormulaCell;
import jxl.Range;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.Formula;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import cn.com.job51.dynamicsetdb.CustomerContextHolder;
import cn.com.job51.relation.model.ActivityPersonTools;
import cn.com.job51.relation.service.ActivityPersonToolsService;
import cn.com.maxtech.activity.model.ActivityPersons;
import cn.com.maxtech.activity.service.ActivityPersonService;
import cn.com.maxtech.activity.service.ExamResultService;
import cn.com.maxtech.activity.util.MemcacheExport;
import cn.com.maxtech.common.action.CommonAction;
import cn.com.maxtech.common.memcached.MemcachedUtil;
import cn.com.maxtech.common.util.OSUtil;
import cn.com.maxtech.common.util.PropertiesUtil;
import cn.com.maxtech.enterprise.model.Enterprise;
import cn.com.maxtech.report.util.ReportBase;
import cn.com.maxtech.solutions.model.SolutionsTools;
import cn.com.maxtech.solutions.model.SolutionsZd;
import cn.com.maxtech.solutions.service.SolutionsToolsService;
import cn.com.maxtech.solutions.service.SolutionsZdService;
import cn.com.maxtech.tools.model.Tools;
import cn.com.maxtech.tools.service.ToolsService;
import cn.com.maxtech.util.Maxtech;
import cn.com.zhiding.template.model.TReportTemplate;
import cn.com.zhiding.template.model.TReportTemplateEnActToolOrSol;
import cn.com.zhiding.template.service.TReportTemplateEnActToolOrSolService;
import cn.com.zhiding.template.service.TReportTemplateService;
import cn.emay.mina.filter.reqres.Request;

public class ExportUtil {
	Logger logger = LoggerFactory.getLogger(ExportUtil.class);
	// static public final int status_count = 0;//显示企业HR分数导出状态
	static public final int EXPORTSCORE_INIT = 0;// 初始化状态
	static public final int EXPORTSCORE_GETPEOPLES = 1;// 获取导出人员list
	static public final int EXPORTSCORE_CHARGE = 2;// 进行扣费
	static public final int EXPORTSCORE_EXPORT = 3;// 导出
	static public final int EXPORTSCORE_FINISH = 4;// 结束
	
	private static final String QUOTATION_MARKS = "\"";  //双引号   yxr
	private static final String COMMA = ",";   //逗号    yxr

	private ExamResultService erService = (ExamResultService) Maxtech
			.getInstance().getBean(ExamResultService.ID_NAME);
	private ActivityPersonToolsService aptService = (ActivityPersonToolsService) Maxtech
			.getInstance().getBean(ActivityPersonToolsService.ID_NAME);
	private ActivityPersonService apService = (ActivityPersonService) Maxtech
			.getInstance().getBean(ActivityPersonService.ID_NAME);

	// protected String template_path =
	// this.getClass().getResource("/").getPath();// WEB-INF/classes目录

	public String getExcelTemplateFile(String template_excel) {
		String path = "";
		String template_path = this.getClass().getResource("/").getPath();

		if (!OSUtil.isLinux) {
			path = template_path.substring(1, template_path.length()) + "/xls/"
					+ template_excel;
		} else {
			path = template_path + File.separator + "xls" + File.separator
					+ template_excel;
		}
		return path;
	}

	/**
	 * 
	 * @title: 获取指定表的字段名</p>
	 * @Description: </p>
	 * @param tableName表名
	 * @return
	 * @author baixf
	 * @date 2016年9月7日
	 */
	public ArrayList getTableColumnByTableName(String tableName) {
		ArrayList lColumnName = new ArrayList();
		String sql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE table_name = '"
				+ tableName + "'";
		List<Object[]> list = erService.getInfoBySql(sql);
		for (Object obj : list)
			lColumnName.add(obj.toString());
		return lColumnName;
	}

	public List getTableDataByApIdTableNameToolId(Long apId, String tableName,
			String pro) {
		String sql = "select * from " + tableName
				+ " where activityPerson_id = " + apId;
		String[] str = pro.split("_");
		if ("t".equals(str[0])) {
			sql += " and tool_id = " + str[1];
		}
		List<ArrayList> listResult = erService.getTableContent(sql);
		return listResult;
	}

	public List getTableDataByApIdTableName(Long apId, String tableName) {
		String sql = "select * from " + tableName
				+ " where activityPerson_id = " + apId;
		if (tableName.equalsIgnoreCase("activity_persons")) {
			sql = "select * from " + tableName + " where id = " + apId;
		}
		List<ArrayList> listResult = erService.getTableContent(sql);
		return listResult;
	}

	public String getModuleId(ArrayList columnNameList, ArrayList apResult) {
		int moduleIdIndex = 0;
		for (int i = 0; i < columnNameList.size(); i++) {
			if ("module_id".equalsIgnoreCase(columnNameList.get(i).toString())) {
				moduleIdIndex = i;
				break;
			}
		}
		return apResult.get(moduleIdIndex).toString();
	}

	public HashMap<String, String> getTableInfoByAPIDProID(String tableName,
			Long apId, String pro) {
		HashMap tableData = new HashMap();

		List<ArrayList> resultList = getTableDataByApIdTableNameToolId(apId,
				tableName, pro);
		ArrayList columnList = erService.getTableColumn(tableName);
		if (resultList == null || columnList == null || resultList.size() == 0
				|| columnList.size() == 0)
			return tableData;

		for (ArrayList apResult : resultList) {
			String moduleId = getModuleId(columnList, apResult);
			int i = 0;
			for (Object data : apResult) {
				String columnName = columnList.get(i).toString();
				String dataStr = "";
				if (data != null)
					dataStr = data.toString();

				tableData.put(moduleId + "_" + columnName, dataStr);
				i++;
			}
		}
		return tableData;
	}

	public HashMap<String, String> getTableInfoByAPID(String tableName,
			Long apId) {
		HashMap tableData = new HashMap();

		List<ArrayList> resultList = getTableDataByApIdTableName(apId,
				tableName);
		ArrayList columnList = erService.getTableColumn(tableName);
		if (resultList == null || columnList == null || resultList.size() == 0
				|| columnList.size() == 0)
			return tableData;

		for (ArrayList apResult : resultList) {
			int i = 0;
			for (Object data : apResult) {
				String columnName = columnList.get(i).toString().toLowerCase();
				String dataStr = "";
				if (data != null)
					dataStr = data.toString();

				tableData.put(columnName, dataStr);
				i++;
			}
		}
		return tableData;
	}

	/**
	 * 
	 * @title: 获取功能列表</p>
	 * @Description: </p>
	 * @param ws
	 * @param func_list
	 * @return
	 * @author baixf
	 * @date 2016年11月3日
	 */
	public int getFunList(WritableSheet ws, HashMap<Integer, String> func_list,
			HashMap<Integer, String> formula_list) {
		try {
			int beginRow = 0;
			for (int i = 0; i < 10; i++) {
				WritableCell wc = ws.getWritableCell(0, i);
				if (wc.getType() == CellType.LABEL) {
					Label l = (Label) wc;
					if (l.getContents().contains("%#")) {
						beginRow = i;
						break;
					}
				}
			}

			if (beginRow == 0) {
				System.out.println("没有找到导出分数的公式！");
				return beginRow;
			}

			for (int i = 0; i < 100; i++) {
				WritableCell wc = ws.getWritableCell(i, beginRow);

				if (wc.getType() == CellType.LABEL) {
					Label l = (Label) wc;
					System.out.println(l.getContents() + "");
					if (l.getContents() != null || !"".equals(l.getContents())) {
						if (l.getContents().startsWith("formula="))// 对excel中自定义公式的特殊处理
						{
							String formula = l.getContents().substring(8);
							formula_list.put(i, formula);
						} else {
							func_list.put(i, l.getContents());
						}
					} else {
						// func_list.add("");
						// l.setString("");
						break;
					}
					// }else if( wc.getType() == CellType.STRING_FORMULA ){
					// FormulaCell nfc = (FormulaCell) wc;
					// System.out.println(
					// "nfc.getFormula()==="+nfc.getFormula()+"" );
					// // Formula n = nfc.getFormula();
					// if(nfc.getContents() != null ||
					// !"".equals(nfc.getContents())){
					// formula_list.put(i, nfc);
					// }

				} else {
					break;
				}
			}

			return beginRow;

		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}

	}

	/*
	 * public String export(String product, List<Long> list, Long enId, Tools
	 * tool){ //获取产品类型，如果是专业题，特殊处理,所有专业题默认使用统一模板 boolean isZYTool = false; if(
	 * tool != null && tool.getToolsType()==8 ) isZYTool = true; String fileName
	 * = product+"_"+enId+".xls"; if( isZYTool ) fileName = "t_zy_"+enId+".xls";
	 * String path = getExcelTemplateFile(fileName); File file = new File(path);
	 * if (!file.exists()) { System.out.println("没有企业定制的导出模板，将使用产品的默认模板导出！");
	 * fileName = product+".xls"; if( isZYTool ) fileName = "t_zy.xls"; path =
	 * getExcelTemplateFile(fileName);
	 * 
	 * file = new File(path); if(!file.exists()){
	 * System.out.println("未找到模板文件！------"+product+".xls"); return "Error1"; } }
	 * 
	 * try { String tmp_excel = ""; Workbook rwb = Workbook.getWorkbook(file);
	 * Date now = new Date(); if (!OSUtil.isLinux) { tmp_excel = "c:\\xls\\" +
	 * now.getTime() + fileName; } else { tmp_excel = template_path +
	 * File.separator + now.getTime() + fileName; } File tempfile = new
	 * File(tmp_excel); // File tempfile = new File(url+"tempfile.xls");
	 * WritableWorkbook wwb = Workbook.createWorkbook(tempfile, rwb);
	 * WritableSheet ws = wwb.getSheet(0);
	 * 
	 * WritableFont wFont = new WritableFont(WritableFont.ARIAL, 11);// table样式
	 * WritableCellFormat wCellFormat = new WritableCellFormat(wFont);//
	 * tableCell样式 wCellFormat.setAlignment(jxl.format.Alignment.CENTRE);//
	 * 水平方向居中 wCellFormat
	 * .setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);// 竖直方向居中
	 * wCellFormat.setBorder(jxl.format.Border.ALL,
	 * jxl.format.BorderLineStyle.THIN);// 边框设置 wCellFormat.setWrap(true);//
	 * 自动换行
	 * 
	 * int beginRow = 0; for(int i=0; i<10; i++){ WritableCell wc =
	 * ws.getWritableCell(0, i); if (wc.getType() == CellType.LABEL){ Label l =
	 * (Label) wc; if(l.getContents().contains("%#")){ beginRow = i; break; } }
	 * }
	 * 
	 * if(beginRow == 0){ return "Error2"; }
	 * 
	 * //可能需要从分库取数据 String dbid = ""; //先将函数公式这行的内容替换掉，并保存函数公式，然后再批量处理添加
	 * 应该判断一下如果取出来是“”就直接break List<String> func_list = new
	 * ArrayList<String>();//储存函数公式 HashMap hApResultData = new HashMap();
	 * for(int i=0; i<100; i++){ WritableCell wc = ws.getWritableCell(i,
	 * beginRow); if (wc.getType() == CellType.LABEL){ Label l = (Label) wc;
	 * if(l.getContents() != null && l.getContents().contains("%#")){
	 * func_list.add(l.getContents()); l.setString(analyzeFunc(hApResultData,
	 * l.getContents(), list.get(0), product, tool)); }else{ //
	 * func_list.add(""); // l.setString(""); break; } }else{ //
	 * func_list.add(""); break; } } hApResultData.clear(); hApResultData=null;
	 * 
	 * for(int i=1; i<list.size(); i++){ //因为已经用第一个人将函数公式一行替换了，所以下标从1开始
	 * hApResultData = new HashMap(); double beginTime =
	 * System.currentTimeMillis(); //可能需要从分库取数据 beginRow++; for(int j=0;
	 * j<func_list.size(); j++){ if("".equals(func_list.get(j))){ ws.addCell(new
	 * Label(j, beginRow, "", wCellFormat)); continue; } String value =
	 * analyzeFunc(hApResultData,func_list.get(j), list.get(i), product, tool);
	 * ws.addCell(new Label(j, beginRow, value, wCellFormat)); }
	 * if(!"1".equals(dbid)){ new
	 * CustomerContextHolder().setDynamicDataSource("0"); //还原成主库 } double
	 * endTime = System.currentTimeMillis();
	 * System.out.println("企业HR分数导出："+i+"/"
	 * +list.size()+"填写一个人"+list.get(i)+"的信息耗时："+(endTime-beginTime));
	 * 
	 * hApResultData.clear(); hApResultData=null; }
	 * 
	 * wwb.write(); wwb.close(); rwb.close();
	 * 
	 * System.out.println("tempfile:" + tempfile.getPath()); return
	 * tempfile.getPath();
	 * 
	 * } catch (Exception e) { e.printStackTrace(); return null; }
	 * 
	 * 
	 * }
	 */
	public String export_old(String product, List<ActivityPersons> list,
			Long enId) {
		// 获取产品类型，如果是专业题，特殊处理,所有专业题默认使用统一模板
		Tools tools_zy = null;
		boolean isZYTool = false;
		String[] pro = product.split("_");
		if ("t".equals(pro[0])) {
			ToolsService tService = (ToolsService) Maxtech.getInstance()
					.getBean(ToolsService.ID_NAME);
			tools_zy = tService.getById(Long.parseLong(pro[1]));
			if (tools_zy.getToolsType() == 8)
				isZYTool = true;
		}
		String fileName = product + "_" + enId + ".xls";
		if (isZYTool)
			fileName = "t_zy_" + enId + ".xls";
		String path = getExcelTemplateFile(fileName);
		System.out.println("path----->" + path);
		File file = new File(path);
		if (!file.exists()) {
			System.out.println("没有企业定制的导出模板，将使用产品的默认模板导出！pppp");
			System.out.println("path----->" + path);
			fileName = product + ".xls";
			if (isZYTool)
				fileName = "t_zy.xls";
			path = getExcelTemplateFile(fileName);
			System.out.println("path2----->" + path);
			file = new File(path);
			if (!file.exists()) {
				System.out.println("未找到模板文件！------" + product + ".xls");
				return "Error1";
			}
		}

		try {
			String template_path = this.getClass().getResource("/").getPath();
			String tmp_excel = "";
			Workbook rwb = Workbook.getWorkbook(file);
			Date now = new Date();
			// if (!OSUtil.isLinux) {
			// tmp_excel = "c:\\xls\\" + now.getTime() + fileName;
			// } else {
			// tmp_excel = template_path + File.separator + now.getTime()
			// + fileName;
			// }
			tmp_excel = System.getProperty("java.io.tmpdir") + File.separator
					+ now.getTime() + fileName;
			System.out.println("HR导出临时文件：" + tmp_excel);
			File tempfile = new File(tmp_excel);
			// File tempfile = new File(url+"tempfile.xls");
			WritableWorkbook wwb = Workbook.createWorkbook(tempfile, rwb);
			WritableSheet ws = wwb.getSheet(0);

			WritableFont wFont = new WritableFont(WritableFont.ARIAL, 11);// table样式
			WritableCellFormat wCellFormat = new WritableCellFormat(wFont);// tableCell样式
			wCellFormat.setAlignment(jxl.format.Alignment.CENTRE);// 水平方向居中
			wCellFormat
					.setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);// 竖直方向居中
			wCellFormat.setBorder(jxl.format.Border.ALL,
					jxl.format.BorderLineStyle.THIN);// 边框设置
			wCellFormat.setWrap(true);// 自动换行

			int beginRow = 0;
			for (int i = 0; i < 10; i++) {
				WritableCell wc = ws.getWritableCell(0, i);
				if (wc.getType() == CellType.LABEL) {
					Label l = (Label) wc;
					if (l.getContents().contains("%#")) {
						beginRow = i;
						break;
					}
				}
			}

			if (beginRow == 0) {
				return "Error2";
			}

			// 可能需要从分库取数据
			String dbid = "";
			CustomerContextHolder cch = new CustomerContextHolder();
			dbid = String.valueOf(cch
					.getDbIdByseqCode(list.get(0).getSeqCode()));
			System.out.println("dbid--->" + dbid);
			if (!"1".equals(dbid)) {
				cch.setDynamicDataSource(dbid);
			}
			// 先将函数公式这行的内容替换掉，并保存函数公式，然后再批量处理添加 应该判断一下如果取出来是“”就直接break
			List<String> func_list = new ArrayList<String>();// 储存函数公式
			HashMap hApResultData = new HashMap();// 记录表和表字段对应值
			HashMap hApInfoData = new HashMap();// 记录测评人信息
			for (int i = 0; i < 100; i++) {
				WritableCell wc = ws.getWritableCell(i, beginRow);
				if (wc.getType() == CellType.LABEL) {
					Label l = (Label) wc;
					if (l.getContents() != null
							&& l.getContents().contains("%#")) {
						func_list.add(l.getContents());
						l.setString(analyzeFunc(hApResultData, hApInfoData,
								l.getContents(), list.get(0).getId(), product,
								tools_zy, new Integer(dbid).intValue()));
					} else {
						// func_list.add("");
						// l.setString("");
						break;
					}
				} else {
					// func_list.add("");
					break;
				}
			}
			hApResultData.clear();
			hApResultData = null;
			hApInfoData.clear();
			hApInfoData = null;
			new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库

			for (int i = 1; i < list.size(); i++) { // 因为已经用第一个人将函数公式一行替换了，所以下标从1开始
				hApResultData = new HashMap();
				double beginTime = System.currentTimeMillis();
				// 可能需要从分库取数据
				dbid = String.valueOf(cch.getDbIdByseqCode(list.get(i)
						.getSeqCode()));
				if (!"1".equals(dbid)) {
					cch.setDynamicDataSource(dbid);
				}
				beginRow++;
				for (int j = 0; j < func_list.size(); j++) {
					if ("".equals(func_list.get(j))) {
						ws.addCell(new Label(j, beginRow, "", wCellFormat));
						continue;
					}
					String value = analyzeFunc(hApResultData, hApInfoData,
							func_list.get(j), list.get(i).getId(), product,
							tools_zy, new Integer(dbid).intValue());
					ws.addCell(new Label(j, beginRow, value, wCellFormat));
				}
				if (!"1".equals(dbid)) {
					new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
				}
				double endTime = System.currentTimeMillis();
				System.out.println("企业HR分数导出：" + i + "/" + list.size()
						+ "填写一个人" + list.get(i).getId() + "的信息耗时："
						+ (endTime - beginTime));

				hApResultData.clear();
				hApResultData = null;
				hApInfoData.clear();
				hApInfoData = null;
			}

			wwb.write();
			wwb.close();
			rwb.close();

			System.out.println("tempfile:" + tempfile.getPath());
			return tempfile.getPath();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * 获取excelPath
	 * @author Yxr
	 * @param product
	 * 		: 产品id
	 * @param enId
	 * 		 : 企业id
	 * @return
	 */
	public String excelPath(String product,Long enId,Long reportId){
		//先判断是否有企业定制模板 目的只excelPath
		/**产品类别 及 id*/
		String[] args=product.split("_");
		/** 报告地址*/
		String excelPath=null;
		/** 非定制报告模板集合 */
		List<TReportTemplate> tReportTemplates = null;
		TReportTemplateService tReportTemplateService = (TReportTemplateService) Maxtech
														.getInstance().getBean(TReportTemplateService.ID_NAME);
		TReportTemplateEnActToolOrSolService enActToolOrSolService = (TReportTemplateEnActToolOrSolService) Maxtech
													.getInstance().getBean(TReportTemplateEnActToolOrSolService.ID_NAME);
		/** 企业订制表中 获取模板id **/
		List<TReportTemplateEnActToolOrSol> enActToolOrSolList = new ArrayList<TReportTemplateEnActToolOrSol>();
		tReportTemplates = tReportTemplateService.findAllTlistByReportIdExport(reportId);
		if(tReportTemplates!=null&&!tReportTemplates.isEmpty()){
			if(tReportTemplates.get(0).getExcelPath()!=null){
			excelPath = tReportTemplates.get(0).getExcelPath();}
			}else{
		if("t".equals(args[0])){
			//enActToolOrSolList = enActToolOrSolService.findByEnterpriceAndToolsExport(enId, Long.parseLong(args[1]),reportId);
			enActToolOrSolList = enActToolOrSolService.findByEnterpriceAndToolsExport(enId, Long.parseLong(args[1]));
			if(enActToolOrSolList.isEmpty()||enActToolOrSolList.size()==0){
				System.out.println("未查到定制模板，查询工具 默认模板");
				tReportTemplates = tReportTemplateService.findAllTlistExport(Long.parseLong(args[1]))	;
				//tReportTemplates = tReportTemplateService.findAllTlistByReportIdExport(reportId);
				if(tReportTemplates!=null&&!tReportTemplates.isEmpty()){
					if(tReportTemplates.get(0).getExcelPath()!=null){
				excelPath = tReportTemplates.get(0).getExcelPath();}
				}
			}else{
				excelPath = enActToolOrSolList.get(0).getReportTemplate().getExcelPath();
			}
			
		}else{
			enActToolOrSolList = enActToolOrSolService.findByEnterpriceAndSolutionExport(enId, Long.parseLong(args[1]));
			if(enActToolOrSolList.isEmpty()||enActToolOrSolList.size()==0){
				System.out.println("未查到定制模板，查询方案 默认模板");
			tReportTemplates = tReportTemplateService.findAllSolutionExport(Long.parseLong(args[1]));
				//tReportTemplates = tReportTemplateService.findAllTlistByReportIdExport(reportId);
				if(tReportTemplates!=null&&!tReportTemplates.isEmpty()){
					if(tReportTemplates.get(0).getExcelPath()!=null){
					excelPath = tReportTemplates.get(0).getExcelPath();}}
		}else{
			if(enActToolOrSolList.get(0).getReportTemplate().getExcelPath()!=null){
			excelPath = enActToolOrSolList.get(0).getReportTemplate().getExcelPath();}
		}
			}}
		return excelPath;	
	}


	public int getMemache(String fileName,WritableSheet ws,HashMap<Integer, String> func_list ,
			HashMap<Integer, String> formula_list,Workbook workbook ){
				ExportUtil util = new ExportUtil();
				MemcachedUtil memcachedUtil = MemcachedUtil.getInstance();
				int beginRow = util.getFunList(ws, func_list, formula_list);
				//stringBuffer(ws);
				//System.out.println(stringBuffer(ws));
				Sheet sheet = workbook.getSheet(0);
				//获取数据
				List listHeader = new ArrayList();
				Range[] rangeCell = sheet.getMergedCells(); 
				// sheet.getRows()
				//获取表头
				 for (int i = 2; i <beginRow; i++) {  
			            for (int j = 0; j < sheet.getColumns(); j++) {  
			                String str = null;  
			                str = sheet.getCell(j, i).getContents();  
			                for (Range r : rangeCell) {  
			                    if (i > r.getTopLeft().getRow()  
			                            && i <= r.getBottomRight().getRow()  
			                            && j >= r.getTopLeft().getColumn()  
			                            && j <= r.getBottomRight().getColumn()) {  
			                        str = sheet.getCell(r.getTopLeft().getColumn(),  
			                                r.getTopLeft().getRow()).getContents();  
			                    } 
			                    	
			                } 
			                listHeader.add(str);
			              //  System.out.print(str + "\t");  
			            }  
			           // System.out.println();  
			        }  
			
				workbook.close();
				for(Object str:listHeader){
					System.out.println(str);
				}
				MemcacheExport export = new MemcacheExport();
				export.setFormula_list(formula_list);
				export.setFunc_list(func_list);
				export.setBeginRow(beginRow);
				export.setListHeader(listHeader);
				System.out.println("fileName---------->>"+fileName);
				memcachedUtil.set(fileName, export); //将函数放进缓存里
				return beginRow;
			}
				
	public WritableCellFormat getFont() throws WriteException{
					WritableFont wFont = new WritableFont(WritableFont.ARIAL, 11);// table样式
					WritableCellFormat wCellFormat = new WritableCellFormat(wFont);// tableCell样式
					wCellFormat.setAlignment(jxl.format.Alignment.CENTRE);// 水平方向居中
					wCellFormat.setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);// 竖直方向居中
					wCellFormat.setBorder(jxl.format.Border.ALL,jxl.format.BorderLineStyle.THIN);// 边框设置
					wCellFormat.setWrap(true);// 自动换行
					return wCellFormat;
			}
	
	
	//  获取 file
	/**
	 * 获取 文件，文件名的Map
	 * @param file
	 * @param fileName
	 * @return
	 */
	public Map getFileMap(File file,String fileName){
		Map map = new HashMap();
		map.put("file", file);
		map.put("fileName", fileName);
		map.put("success", "0");
		return map;
	}
	
	/**
	 * 获取excelPath
	 * @param product
	 * @param enId
	 * @param reportId
	 * @return
	 */
	public Map getFile(String product, Long enId,Long reportId){
		Map map = new HashMap();
		Tools tools = null;
		SolutionsZd solutions = null;
		boolean isSolution = false;// 是否是方案还是产品
		Long p_id = null; // 产品id
		String proName = "";// 产品对外名称
		ExportUtil util = new ExportUtil();
		// 产品信息
		String[] pro = product.split("_");
		p_id = Long.parseLong(pro[1]);
		if ("t".equals(pro[0])) {
			ToolsService tService = (ToolsService) Maxtech.getInstance()
					.getBean(ToolsService.ID_NAME);
			tools = tService.getById(p_id);
			proName = tools.getForeignName();
		} else {
			SolutionsZdService sService = (SolutionsZdService) Maxtech
					.getInstance().getBean(SolutionsZdService.ID_NAME);
			solutions = sService.getById(Long.parseLong(pro[1]));
			proName = solutions.getForeignName();
			isSolution = true;
		}

		/** 获取 t_report_template表中的 excelPath内容 */
		logger.info("enId---->" + enId);// 34110
		String excelPath = util.excelPath(product, enId,reportId);
		String fileName = null;
		String path = null;
		File file = null;
		if (excelPath != null && !"".equals(excelPath)) {
			logger.info("excelPath---->>" + excelPath);
			fileName = "ep_" + enId + ".xls";
			fileName = excelPath;
			path = util.getExcelTemplateFile(fileName);
			file = new File(path);
			if (!file.exists()) {
				logger.info("文件不存在");
				map.put("errCode","1");
				return map;
			}else{
		 map = getFileMap(file,fileName);}
		}
		if ( "".equals(excelPath) || null == excelPath ) {

			// 获取产品类型，如果是专业题，特殊处理,所有专业题默认使用统一模板
			boolean isZYTool = false;
			if (tools != null && (tools.getToolsType() == 8 ||19 == tools.getToolsType()))
				isZYTool = true;
			fileName = product + "_" + enId + ".xls";
			logger.info("fileName--->" + fileName);
			if (isZYTool) {
				fileName = "t_zy_" + enId + ".xls";
				logger.info("fileName--->" + fileName);
			}
			
			logger.info("fileName--->" + fileName);
			path = util.getExcelTemplateFile(fileName);
			logger.info("path--->" + path);
			file = new File(path);
			if (!file.exists()) {
				System.out.println("没有企业定制的导出模板，将使用产品的默认模板导出！");
				System.out.println("调用我了");
				fileName = product + ".xls";
				if (isZYTool)
					fileName = "t_zy.xls";
				path = util.getExcelTemplateFile(fileName);
				logger.info("fileName------>" + fileName);
				logger.info("path------>" + path);
				file = new File(path);
				if (!file.exists()) {
					map.put("errCode", "1");
					System.out.println("未找到模板文件！------" + product + ".xls");
					return null;
				}else{
					 map = getFileMap(file,fileName);}
			}else{
				 map = getFileMap(file,fileName);}
		}		
		return map;
	}
	
	
	/**
	 * 获取分数导出 结果的json
	 * @param product
	 * @param producrCode
	 * @param list
	 * @param apid_examresult_map
	 * @param dbIdapListMap
	 * @param enId
	 * @param reportId
	 * @param duplicateApId
	 * @param request
	 * @return json.toString
	 */
	public String exportComment(String product,String producrCode, List<ActivityPersons> list,Map apid_examresult_map,
			HashMap<Integer, List> dbIdapListMap, Long enId,Long reportId,
			List<Long> duplicateApId,HttpServletRequest request ){
		// 获取产品类型，如果是专业题，特殊处理,所有专业题默认使用统一模板
		double begintime = System.currentTimeMillis();
		CustomerContextHolder cch = new CustomerContextHolder();
		Long p_id = null; // 产品id
		String proName = "";// 产品对外名称
		boolean isSolution = false;// 是否是方案还是产品
		HttpSession session  = request.getSession();
				/** 获取  t_report_template表中的 excelPath内容 */
				String excelPath=excelPath(product,enId,reportId);
				/**获取当前日期作为后边模板导出的名称一部分 */
				Date now = new Date();
				String path = null;
				Tools tools = null;
				SolutionsZd solutions = null;
				/**文件名*/
				String fileName = null;
				File file = null;
				Map fileMap = getFile(product, enId, reportId );
				if(fileMap!=null){
					if("1".equals(fileMap)){
						logger.info("文件不存在");
						logger.info("Error1");
						return "Error1";
					}
					fileName = (String) fileMap.get("fileName");
					file = (File) fileMap.get("file");
				}
				// 根据分库数量进行查找
				int dbNum = ExportUtil.getDBNum();
				if (dbNum == -1) {
					System.out.println("status============================"
							+ 0);
					return null;
				}
				
				// 开始准备导出数据
				MemcachedUtil memcachedUtil = MemcachedUtil.getInstance();
				memcachedUtil.delete(fileName);
				MemcacheExport export = (MemcacheExport) memcachedUtil.get(fileName);
				try {
					CommonAction c = new CommonAction();//杨旭日添加的，因为在ActivityToolAction中，继承了CommonAction
					List lists = new ArrayList();
					String tmp_excel = "";
					Workbook rwb = Workbook.getWorkbook(file);
					tmp_excel = System.getProperty("java.io.tmpdir") + File.separator+ now.getTime() + fileName;
					System.out.println("HR导出临时文件：" + tmp_excel);
					System.out.println("HR导出临时文件hhh：" + File.separator);
					// 打开excel
					File tempfile = new File(tmp_excel);
					WritableWorkbook wwb = Workbook.createWorkbook(tempfile, rwb);
					WritableSheet ws = wwb.getSheet(0);
			/*		WritableFont wFont = new WritableFont(WritableFont.ARIAL, 11);// table样式
					WritableCellFormat wCellFormat = new WritableCellFormat(wFont);// tableCell样式
					wCellFormat.setAlignment(jxl.format.Alignment.CENTRE);// 水平方向居中
					wCellFormat.setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);// 竖直方向居中
					wCellFormat.setBorder(jxl.format.Border.ALL,jxl.format.BorderLineStyle.THIN);// 边框设置
					wCellFormat.setWrap(true);// 自动换行
*/					// 获取导出模板中的公式
					HashMap<Integer, String> func_list = new HashMap();
					HashMap<Integer, String> formula_list = new HashMap();
					int beginRow = 0;
					if(export==null){
						beginRow =getMemache(fileName, ws, func_list, formula_list, rwb);
					}else{
						beginRow = export.getBeginRow();
						func_list = export.getFunc_list();
						formula_list = export.formula_list;
					}
					if (beginRow == 0) {
						return "Error2";
					}
					
					int sum_col = func_list.size() + formula_list.size();// excel中有效列数
					if(!dbIdapListMap.isEmpty()){
					beginRow--;// 回退到公式那行，覆盖公式
					int cur_count = 0;
					session.setAttribute("STARTTIME", Calendar.getInstance().getTimeInMillis());
					for (int i = 1; i <= dbIdapListMap.size(); i++) {
						List<Long> aptlist = new ArrayList();
						aptlist = dbIdapListMap.get(new Integer(i));
					for(int j=0;j<aptlist.size();j++){
						Long apid = aptlist.get(j);
						cur_count++;
						beginRow++;
						double beginTime = System.currentTimeMillis();
					 lists.add(getOnePersonExcelScore(i, beginRow, sum_col,
								func_list, formula_list, aptlist.get(j),
								product, tools));
					 session.setAttribute("CURRCOUNT", cur_count);
						double endTime = System.currentTimeMillis();
						System.out.println("数据库:" + i + "企业HR分数导出：" + j + "/"
								+ aptlist.size() + "填写一个人" + aptlist.get(j)
								+ "的信息耗时：" + (endTime - beginTime));
					}}
/*					for (int i = 1; i <= dbNum; i++) {
						try {
							List<Long> aptlist = new ArrayList();
							cch.setDynamicDataSource(new Integer(i).toString());
							aptlist = dbIdapListMap.get(new Integer(i));
							for (int j = 0; j < aptlist.size(); j++) { // 因为已经用第一个人将函数公式一行替换了，所以下标从1开始
								Long apid = aptlist.get(j);
								// 处理重复数据
								if (duplicateApId.contains(apid)) {
									ActivityPersons ap = c.getActivityPersonService()
											.getById(apid);
									String dbid = String.valueOf(new CustomerContextHolder()
													.getDbIdByseqCode(ap.getSeqCode()));
									if (!dbid.equals(new Integer(i).toString()))// 脏数据，该apid不应该在当前数据库中
										continue;
								}
								cur_count++;
								beginRow++;
								double beginTime = System.currentTimeMillis();
								aptlist.size();
								for (int z = 0; z < aptlist.size(); z++) {
									System.out.println(aptlist.get(z));
								}
							 lists.add(getOnePersonExcelScore(i, beginRow, sum_col,
										func_list, formula_list, ws, aptlist.get(j),
										product, tools, wCellFormat));
							 session.setAttribute("CURRCOUNT", cur_count);
								double endTime = System.currentTimeMillis();
								System.out.println("数据库:" + i + "企业HR分数导出：" + j + "/"
										+ aptlist.size() + "填写一个人" + aptlist.get(j)
										+ "的信息耗时：" + (endTime - beginTime));
							}

							aptlist.clear();
						} catch (Exception ex) {
							System.out.println("企业HR导出分数异常，" + ex.toString());
							return "-1_"+ex.toString();
						} finally {
							cch.setDynamicDataSource(new Integer(i).toString());
						}
					}*/}else{
						// 可能需要从分库取数据
						String dbid = "";
						dbid = String.valueOf(cch.getDbIdByseqCode(list.get(0).getSeqCode()));
						System.out.println("dbid---------->>" + dbid);
						if (!"1".equals(dbid)) {
							cch.setDynamicDataSource(dbid);
						}
						lists.add(getOnePersonExcelScore(new Integer(dbid), beginRow, sum_col,
								func_list, formula_list,  list.get(0).getId(),
								product, tools));
						System.out.println("tempfile:" + tempfile.getPath());
						if (!"1".equals(dbid)) {
							new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
						}
					}
					//生成json
					MemcacheExport export2 = (MemcacheExport)memcachedUtil.get(fileName);
					String sorce = "";
					String header = "";
					int arrLength = export2.getListHeader().size();
					JSONArray jsonArray = new JSONArray();
					JSONArray jsonArrayHash = new JSONArray();
					LinkedHashMap hashMap = new LinkedHashMap();
					hashMap.put("Status", "true");
					hashMap.put("Message", "成功");
					int rowNum = ws.getRows();
					int currentRowNum = rowNum-1;
					for(int i=0;i<lists.size();i++){  //循环成绩次数 i 代表 几个人  j 代表 表头循环
						System.out.println("\t"+"***************第"+i+"个人的成绩**********");
						LinkedHashMap hMap = new LinkedHashMap();
						if(producrCode!=null){
							hMap.put("producrCode", producrCode);
							hMap.put("product_id", product);
							hMap.put("testInvokeKey", apid_examresult_map.get(list.get(rowNum-currentRowNum-1).getId().toString()));
						}
						for(int j=0;j<export2.getListHeader().size();j++){
							header = (String) export2.getListHeader().get(j);
							List listSorce  = (List) lists.get(i);
							sorce = (String) listSorce.get(j);
							System.out.println(header+":"+sorce);
							hMap.put(header, sorce);
						}
						jsonArray.add(hMap);
					}
					hashMap.put("resultList", jsonArray.toString());
					jsonArrayHash.add(hashMap);
					System.out.println(jsonArray.toString());
					System.out.println(jsonArrayHash.toString());
					//解析json
					/*JSONArray jsonArr = JSONArray.fromObject(jsonArray.toString());
		            String a[] = new String[export2.getListHeader().size()];
		            for(int i=0;i<jsonArr.size();i++){//i 代表几个人； j代表 表头
		            	for(int j=0;j<export2.getListHeader().size();j++){
							header = (String) export2.getListHeader().get(j);
							a[j] = jsonArr.getJSONObject(i).getString(header);
							System.out.println(a[j]);
						}
		            }*/
				//	wwb.write();
					wwb.close();
					rwb.close();
					cch.setDynamicDataSource("0");
					SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
					String output_fileName = proName.toString() + "_"
							+ fmt.format(new Date());
					System.out.println("output_fileName---------->" + output_fileName);
					logger.info("output_fileName---------->" + output_fileName);
					double endtime = System.currentTimeMillis();
					System.out.println("整个导出耗时：" + (endtime - begintime));
					return jsonArrayHash.toString();

				} catch (Exception e) {
					e.printStackTrace();
					try {
						cch.setDynamicDataSource("0");
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				return null;

	}

	
	
	public String export(String product, List<ActivityPersons> list, Long enId,Long reportId) {
		// 获取产品类型，如果是专业题，特殊处理,所有专业题默认使用统一模板
		/** 获取  t_report_template表中的 excelPath内容 */
		String excelPath=excelPath(product,enId,reportId);
		/**获取当前日期作为后边模板导出的名称一部分 */
		Date now = new Date();
		String path = null;
		Tools tools = null;
		/**文件名*/
		String fileName = null;
		File file = null;
		if(excelPath!=null&& !"".equals(excelPath)){
			logger.info("excelPath---->>"+excelPath);
			fileName = excelPath;
			path = getExcelTemplateFile(fileName);
			file = new File(path);
			if(!file.exists()){
				logger.info("Error1");
				return "Error1";
			}
		}
		if(excelPath==null || "".equals(excelPath)){
			logger.info("excelPath---->>"+excelPath);
		// -----------------------分界线
		/*Tools tools = null;杨旭日注释*/
		boolean isZYTool = false;
		String[] pro = product.split("_");
		if ("t".equals(pro[0])) {
			ToolsService tService = (ToolsService) Maxtech.getInstance()
					.getBean(ToolsService.ID_NAME);
			tools = tService.getById(Long.parseLong(pro[1]));
			if (tools.getToolsType() == 8|| 19==tools.getToolsType())
				isZYTool = true;
		}
		 fileName = product + "_" + enId + ".xls";
		if (isZYTool)
			fileName = "t_zy_" + enId + ".xls";
		/*String path = getExcelTemplateFile(fileName);杨旭日注释*/
		 path = getExcelTemplateFile(fileName);
		/*Fil file = new File(path);杨旭日注释*/
			file = new File(path);
		if (!file.exists()) {
			System.out.println("没有企业定制的导出模板，将使用产品的默认模板导出！");
			System.out.println("path--->"+path);
			fileName = product + ".xls";
			if (isZYTool)
				fileName = "t_zy.xls";
			path = getExcelTemplateFile(fileName);
			file = new File(path);
			if (!file.exists()) {
				System.out.println("未找到模板文件！------" + product + ".xls");
				return "Error1";
			}
		}
		}
		try {
			String template_path = this.getClass().getResource("/").getPath();
			String tmp_excel = "";
			tmp_excel = System.getProperty("java.io.tmpdir") + File.separator
					+ now.getTime() + fileName;
			System.out.println("HR导出临时文件：" + tmp_excel);
			File tempfile = new File(tmp_excel); //新建 xls文件
			// File tempfile = new File(url+"tempfile.xls");
			Workbook rwb = Workbook.getWorkbook(file); // 创建excel 并写入数据 --》    创建工作簿
			
			//创建sheet，名为sheet1，索引为0
			// WritableSheet sheet = workbook.createSheet("sheet1", 0);
			WritableWorkbook wwb = Workbook.createWorkbook(tempfile, rwb);//读取Excel文件内容----》 创建工作簿
			WritableSheet ws = wwb.getSheet(0); //获取工作表
			WritableFont wFont = new WritableFont(WritableFont.ARIAL, 11);// table样式
			WritableCellFormat wCellFormat = new WritableCellFormat(wFont);// tableCell样式
			wCellFormat.setAlignment(jxl.format.Alignment.CENTRE);// 水平方向居中
			wCellFormat.setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);// 竖直方向居中
			wCellFormat.setBorder(jxl.format.Border.ALL,jxl.format.BorderLineStyle.THIN);// 边框设置
			wCellFormat.setWrap(true);// 自动换行

			HashMap<Integer, String> func_list = new HashMap();
			HashMap<Integer, String> formula_list = new HashMap();
			int beginRow = getFunList(ws, func_list, formula_list);
			System.out.println("beginRow------>>" + beginRow);
			if (beginRow == 0) {
				return "Error2";
			}
			// beginRow--;//回退到公式那行，覆盖公式
			int sum_col = func_list.size() + formula_list.size();// excel中有效列数
			// 可能需要从分库取数据
			String dbid = "";
			CustomerContextHolder cch = new CustomerContextHolder();
			dbid = String.valueOf(cch.getDbIdByseqCode(list.get(0).getSeqCode()));
			System.out.println("dbid---------->>" + dbid);
			if (!"1".equals(dbid)) {
				cch.setDynamicDataSource(dbid);
			}
			writeOnePersonExcelScore(new Integer(dbid), beginRow, sum_col,func_list, formula_list, ws, list.get(0).getId(), product,
					tools, wCellFormat);
			wwb.write();
			wwb.close();
			rwb.close();

			System.out.println("tempfile:" + tempfile.getPath());
			if (!"1".equals(dbid)) {
				new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
			}
			return tempfile.getPath();

		} catch (Exception e) {
			try {
				new CustomerContextHolder().setDynamicDataSource("0");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} // 还原成主库
			e.printStackTrace();
			return null;
		}

	}
	
	
	/**
	 *（分数导出优化使用） 获取一个人的成 结果
	 * @param dbid
	 * @param beginRow
	 * @param func_count
	 * @param func_list
	 * @param formula_list
	 * @param ws
	 * @param aptid
	 * @param product
	 * @param tools
	 * @param wCellFormat
	 * @return
	 * @throws Exception
	 */
	public List getOnePersonExcelScore(int dbid, int beginRow,
			int func_count, HashMap<Integer, String> func_list,
			HashMap<Integer, String> formula_list, 
			Long aptid, String product, Tools tools
		) throws Exception {
		CustomerContextHolder cch = new CustomerContextHolder();
		cch.setDynamicDataSource(dbid + "");
		HashMap hApResultData = new HashMap();
		HashMap hApInfoData = new HashMap();
		List list = new ArrayList();
		for (int k = 0; k < func_count; k++) {
			if (func_list.get(k) != null) {
				if ("".equals(func_list.get(k))) {
					list.add("");
					continue;
				}
				Calendar fun_begin = Calendar.getInstance();
				String cellValue = "";
				String pdUse = "";
				try {
					cellValue = analyzeFunc(hApResultData, hApInfoData,
							func_list.get(k), aptid, product, tools, dbid);
					list.add(cellValue);
							
				} catch (Exception ex) {
					System.out.println("aptid=" + aptid + "公式："
							+ func_list.get(k) + "成绩异常......");
					ex.printStackTrace();
					cellValue = "N/A";
					list.add(cellValue);
				}
				/*if ((func_list.get(k).startsWith("%#MARK") || (func_list.get(k)
						.startsWith("%#MSCORE"))
						&& cellValue != null
						&& !("".equals(cellValue))) && isFloat(cellValue))// 如果是分数，设置为数字格式
				{
					jxl.write.Number lb2 = new jxl.write.Number(k, beginRow,
							new Float(cellValue).intValue(), wCellFormat);
				//	ws.addCell(lb2);
					//list.add(lb2.toString());
				} else
					ws.addCell(new Label(k, beginRow, cellValue, wCellFormat));*/
			}
			
			
			if (formula_list.get(k) != null) {
				String formula = formula_list.get(k);
				String[] cloumStrAll = formula.split(";");
				System.out.println("cloumStrAll长度--》"+cloumStrAll.length);
				for(int i=0;i<cloumStrAll.length;i++){
					String[] cloumStr = cloumStrAll[i].split(":");
					int cloum = excelColStrToNum(cloumStr[0],cloumStr[0].length());
					Object score = list.get(cloum-1);
					boolean flag = isInclude(score+"",cloumStr[1]);
					if(flag){
						list.add(cloumStr[2]);
						System.out.println(cloumStr[2]);
						break;
					}
				}
			}
		}
		hApResultData.clear();
		hApInfoData.clear();
		hApResultData = null;
		hApInfoData = null;
		return list;
	}
	
	 /**
	  * 将 英文转换为数字
     * Excel column index begin 1
     * @param colStr
     * @param length
     * @return
     */
    public static int excelColStrToNum(String colStr, int length) {
        int num = 0;
        int result = 0;
        for(int i = 0; i < length; i++) {
            char ch = colStr.charAt(length - i - 1);
            num = (int)(ch - 'A' + 1) ;
            num *= Math.pow(26, i);
            result += num;
        }
        return result;
    }
    
    
    /** 
     *  拼接判断语句
     * @author: Longjun 
     * @Description: 将${money>=2000&&money<=4000}字符串截取成"money>=2000&&money<=4000"， 
     * 然后判断一个数值字符串是否在此区间内 
     * @date:2016年3月21日 上午11:25:32 
     */  
    public static Boolean isInclude(String elValue,String elString){  
        String el =elValue+elString;  
        ScriptEngineManager manager = new ScriptEngineManager();  
        ScriptEngine engine = manager.getEngineByName("js");  
        engine.put("money",elValue);  
        boolean eval = false;  
        try {  
            eval = (Boolean) engine.eval(el);  
        } catch (Exception e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        }       
        return eval;  
    }  
    
    
    

	/**
	 * 
	 * @param dbid
	 * @param beginRow
	 * @param func_count
	 * @param func_list
	 * @param formula_list
	 * @param ws
	 * @param aptid
	 * @param product
	 * @param tools
	 * @param wCellFormat
	 * @throws Exception
	 */
	public void writeOnePersonExcelScore(int dbid, int beginRow,
			int func_count, HashMap<Integer, String> func_list,
			HashMap<Integer, String> formula_list, WritableSheet ws,
			Long aptid, String product, Tools tools,
			WritableCellFormat wCellFormat) throws Exception {
		CustomerContextHolder cch = new CustomerContextHolder();
		cch.setDynamicDataSource(dbid + "");
		HashMap hApResultData = new HashMap();
		HashMap hApInfoData = new HashMap();
		// double beginTime = System.currentTimeMillis();
		for (int k = 0; k < func_count; k++) {
			if (func_list.get(k) != null) {
				if ("".equals(func_list.get(k))) {
					ws.addCell(new Label(k, beginRow, "", wCellFormat));
					continue;
				}
				Calendar fun_begin = Calendar.getInstance();
				String cellValue = "";
				try {
					cellValue = analyzeFunc(hApResultData, hApInfoData,
							func_list.get(k), aptid, product, tools, dbid);
							
				} catch (Exception ex) {
					System.out.println("aptid=" + aptid + "公式："
							+ func_list.get(k) + "成绩异常......");
					ex.printStackTrace();
					cellValue = "N/A";
				}
				System.out.println("公式"
						+ func_list.get(k)
						+ "耗时:"
						+ (System.currentTimeMillis() - fun_begin
								.getTimeInMillis()));
				if ((func_list.get(k).startsWith("%#MARK") || (func_list.get(k)
						.startsWith("%#MSCORE"))
						&& cellValue != null
						&& !("".equals(cellValue))) && isFloat(cellValue))// 如果是分数，设置为数字格式
				{
					jxl.write.Number lb2 = new jxl.write.Number(k, beginRow,
							new Float(cellValue).intValue(), wCellFormat);
					ws.addCell(lb2);
				} else
					ws.addCell(new Label(k, beginRow, cellValue, wCellFormat));
			}
			if (formula_list.get(k) != null) {
				String formula = formula_list.get(k);
				formula = formula.replace("{$row$}", (beginRow + 1) + "");
				ws.addCell(new Formula(k, beginRow, formula, wCellFormat));
			}
		}
		hApResultData.clear();
		hApInfoData.clear();
		hApResultData = null;
		hApInfoData = null;
		return;
	}

	public boolean isFloat(String str) {
		try {
			new Float(str).intValue();
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	// 解析函数
	public String analyzeFunc(HashMap hApResultData, HashMap hApInfoData,
			String func, Long apId, String pro, Tools tools_zy, int dbid)
			throws Exception {
		String result = "";
		func = func.substring(2, func.length() - 2);
		String str[] = func.split("\\(");
		func = str[0];
		String parameters = str[1].substring(0, str[1].length() - 1);
		String[] parameter = parameters.split(",");

		String tableName = "";
		String column = "";
		if ("SINGLEDATA".equals(func)) {
			tableName += parameter[0];
			column += parameter[1];
			if (parameter.length > 2) {
				String toolId = parameter[2];
				result = singleData(hApInfoData, tableName, column, toolId,
						apId, pro, dbid);
			} else {
				result = singleData(hApInfoData, tableName, column, "", apId,
						pro, dbid);
			}
		} else if ("SINGLEDATA_ZY".equals(func)) {
			tableName += parameter[0];
			column += parameter[1];
			if (parameter.length > 2) {
				String toolId = parameter[2];
				result = singleData(hApInfoData, tableName, column, tools_zy
						.getId().toString(), apId, pro, dbid);
			} else {
				result = singleData(hApInfoData, tableName, column, "", apId,
						pro, dbid);
			}
		} else if ("MARK".equals(func)) {
			tableName += parameter[0];
			column += parameter[1];
			String module_id = parameter[2];
			result = getMark(hApResultData, tableName, column, module_id, apId,
					pro);
		} else if ("VALID".equals(func)) {
			result = getValid(apId);
		} else if ("DESC".equals(func)) {
			tableName += parameter[0];
			column += parameter[1];
			String module_id = parameter[2];
			result = getMark(hApResultData, tableName, column, module_id, apId,
					pro);
		} else if ("DATE".equals(func)) {
			tableName += parameter[0];
			column += parameter[1];
			result = getDate(tableName, column, apId);
		} else if ("SUBMITTIME".equals(func)) { // 取方案的提交时间（自己加的函数）
			List<ActivityPersonTools> aptList = null;
			String[] product = pro.split("_");
			if ("t".equals(product[0])) {
				aptList = aptService.getByApIdToolIdOrderByCompleteDate(apId,
						new Integer(product[1]));
			} else {
				SolutionsToolsService solutionToolsService = (SolutionsToolsService) Maxtech
						.getInstance().getBean(SolutionsToolsService.ID_NAME);
				List<SolutionsTools> solutionTools = solutionToolsService
						.getBySolutionId(Long.parseLong(product[1]));
				String solutionToolIDs = "";
				for (SolutionsTools solutionTool : solutionTools) {
					solutionToolIDs += solutionTool.getTools().getId() + ",";
				}
				if (solutionToolIDs.length() > 0)
					solutionToolIDs = solutionToolIDs.substring(0,
							solutionToolIDs.length() - 1);
				aptList = aptService
						.getByApIdSolutionToolIdsOrderByCompleteDate(apId,
								solutionToolIDs);
			}

			if (aptList == null) {
				return "";
			}
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			System.out.println("hhhhh--->"+aptList.get(0).getCompleteDate());
			System.out.println("hhhhh--->"+aptList.get(0).getToolName());
			System.out.println("hhhhh--->"+aptList.get(0).getActivityName());
			System.out.println("hhhhh--->"+aptList.get(0).getActivityPerson_id());
			
			result = df.format(aptList.get(0).getCompleteDate()).toString();
		} else if ("CODE".equals(func)) {
			ActivityPersons bean = apService.getById(apId);
			if (bean != null && bean.getUsercode() != null
					&& !"".equals(bean.getUsercode())) {
				result = bean.getUsercode();
			} else {
				result = "";
			}
		} else if ("LEVEL".equals(func)) {
			tableName += parameter[0];
			String condition = parameter[1];
			String module_id = parameter[2];
			column += parameter[3];
			result = getLevel(tableName, condition, module_id, column, apId);
		} else if ("MENTALSTATE".equals(func)) {
			tableName += parameter[0];
			column += parameter[1];
			String module_id = parameter[2];
			String temp = getMark(hApResultData, tableName, column, module_id,
					apId, pro);
			if (temp == null || "".equals(temp)) {
				System.out.println("可能没有此人的作答记录！------apId=" + apId);
				return "";
			}
			Double t_mark = Double.parseDouble(temp);
			if (t_mark < 48.0d) {
				result = "值得关注";
			} else {
				result = "正常";
			}
		} else if ("FANGZUOBI".equals(func)) { // 取防作弊的跳出次数
			ActivityPersons bean = apService.getById(apId);
			if (bean != null && bean.getJumpTimes() != null
					&& !"".equals(bean.getJumpTimes())) {
				result = bean.getJumpTimes().toString();
			} else {
				result = "";
			}
		} else if ("ROLEADVANTAGE".equals(func)) {
			tableName += parameter[0];
			String sorttoolid = parameter[1];
			String version = parameter[2];
			String sortcondition = parameter[3];
			String sortorder = parameter[4];
			String rank = parameter[5];
			String colum = parameter[6];
			String rows = parameter[7];
			result = roleAdvantage(tableName, sorttoolid, version,
					sortcondition, sortorder, rank, colum, rows, apId);
		} else if ("SORTMODULEDESC".equals(func)) {
			tableName += parameter[0];
			String sorttoolid = parameter[1];
			String version = parameter[2];
			String sortcondition = parameter[3];
			String sortorder = parameter[4];
			String rank = parameter[5];
			String condition = parameter[6];
			if (parameter.length > 7) {
				String parentid = parameter[7];
				result = sortModuleDesc(tableName, sorttoolid, version,
						sortcondition, sortorder, rank, condition, parentid,
						apId, pro);
			} else {
				result = sortModuleDesc(tableName, sorttoolid, version,
						sortcondition, sortorder, rank, condition, "", apId,
						pro);
			}
		} else if ("SORTDISC".equals(func)) {
			tableName += parameter[0];
			String sorttoolid = parameter[1];
			String version = parameter[2];
			String sortcondition = parameter[3];
			String sortorder = parameter[4];
			String colum = parameter[5];
			result = sortdisc(tableName, sorttoolid, version, sortcondition,
					sortorder, colum, apId, dbid + "", pro);
		} else if ("COMPARE".equals(func)) { // 处理MAP报告的职业倾向，比较两个分数取分数较高的倾向（自己加的函数）
			tableName += parameter[0];
			column += parameter[1];
			String moduleId = parameter[2];
			String moduleName1 = parameter[3];
			String moduleName2 = parameter[4];
			result = compare(tableName, column, moduleId, moduleName1,
					moduleName2, apId);
		} else if ("COMPARE_PPF".equals(func)) { // 处理MAP报告的职业倾向，比较两个分数取分数较高的倾向（自己加的函数）
			tableName += parameter[0];
			column += parameter[1];
			String moduleId = parameter[2];
			String moduleName1 = parameter[3];
			String moduleName2 = parameter[4];
			result = compare_ppf(hApResultData, tableName, column, moduleId,
					moduleName1, moduleName2, apId, pro);
		} else if ("SORT".equals(func)) {
			tableName += parameter[0];
			String sorttoolid = parameter[1];
			String sortcondition = parameter[2];
			String sortorder = parameter[3];
			String rank = parameter[4];
			column = parameter[5];
			result = sort(tableName, sorttoolid, sortcondition, sortorder,
					rank, column, apId);
		} else if ("SORTRESULT".equals(func)) {
			tableName += parameter[0];
			String toolId = parameter[1];
			String solutioniId = parameter[2];
			column = parameter[3];
			result = sortResult(tableName, toolId, solutioniId, column, apId,
					pro);
		} else if ("SORTRESULT_ZY".equals(func)) {
			tableName += parameter[0];
			String toolId = parameter[1];
			String solutioniId = parameter[2];
			column = parameter[3];
			result = sortResult_zy(tableName, toolId, solutioniId, column,
					apId, pro);
		} else if ("MSCORE".equals(func)) {
			tableName += parameter[0];
			String toolId = parameter[1];
			String moduleId = parameter[2];
			column = parameter[3];
			String floatCalType = "";
			if (parameter.length == 5)
				floatCalType = parameter[4];
			result = mscore(tableName, toolId, moduleId, column, floatCalType,
					apId);
				result = checkExtremeValue(result);	

		} else if ("NINELEVEL".equals(func)) {
			tableName += parameter[0];
			String toolId = parameter[1];
			String moduleId = parameter[2];
			column = parameter[3];
			String floatCalType = "";
			if (parameter.length > 5)
				floatCalType = parameter[4];
			result = mscore(tableName, toolId, moduleId, column, floatCalType,
					apId);
			float score = 0;
			try {
				score = new Float(result).intValue();
			} catch (Exception ex) {
				System.out.println("分数导出错误:" + func);
				System.out.println(ex.toString());
				return "";
			}
			float[] arrStandard = { 4, 11, 23, 40, 60, 77, 89, 96 };
			for (int i = 0; i < arrStandard.length; i++) {
				if (score < arrStandard[i])
					return (i + 1) + "";
			}
			return "9";

		} else if ("WARNING".equals(func)) {
			tableName += parameter[0];
			column += parameter[1];
			String moduleId = parameter[2];
			result = Warning(tableName, column, moduleId, apId);
		} else if ("DEVELOPMENT".equals(func)) {
			tableName += parameter[0];
			String sorttoolid = parameter[1];
			String sortversion = parameter[2];
			String sortcondition = parameter[3];
			String sortorder = parameter[4];
			String rank = parameter[5];
			String rows = parameter[6];
			String dev_type = parameter[7];
			String dev_version = parameter[8];
			result = development(tableName, sorttoolid, sortversion,
					sortcondition, sortorder, rank, rows, dev_type,
					dev_version, apId, pro);
		}
		return result;
	}

	public String singleData(HashMap hApInfoData, String tableName,
			String column, String toolId, Long apId, String product, int dbid) {
		try {
			String result = "";
			HashMap<String, String> hApTableResultData = (HashMap<String, String>) hApInfoData
					.get(tableName);
			if ("activity_persons".equals(tableName)) {
				if (hApTableResultData == null)// 第一次获取数据
				{
					hApTableResultData = getTableInfoByAPID(tableName, apId);
					hApInfoData.put(tableName, hApTableResultData);
				}
				if (hApTableResultData.size() == 0)// 该用户没有数据，异常
					return "";

				result = hApTableResultData.get(column.toLowerCase());
			} else {
				// System.out.println("activity_persons1".compareToIgnoreCase(tableName));
				String sql = "select `" + column + "` from `" + tableName
						+ "` where 1=1 ";
				if (!"".equals(toolId)) {
					sql += " and tool_id = " + toolId;
				}
				if ("activity_persons".compareToIgnoreCase(tableName) == 0) {
					sql += " and id = " + apId;
				} else if (tableName.compareToIgnoreCase("s_zhiding_tools") != 0) {
					sql += " and activityPerson_id = " + apId;
				}
				List<Object[]> list = new ArrayList<Object[]>();
				// new CustomerContextHolder().setDynamicDataSource("2");
				if (tableName.compareToIgnoreCase("s_zhiding_tools") == 0) {
					new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
					list = erService.getInfoBySql(sql);
					new CustomerContextHolder()
							.setDynamicDataSource(new Integer(dbid).toString()); // 还原原来数据库
				} else {
					list = erService.getInfoBySql(sql);
				}
				// String result = "";
				if (list != null && list.size() > 0) {
					result = list.get(0)[0].toString();
				}
			}

			if ("costTimes".compareToIgnoreCase(column) == 0
					&& !"".equals(result)) {
				int time = Integer.parseInt(result);
				int minute = (int) Math.floor(time / 60);
				int second = time % 60;
				if (minute == 0)
					return second + "秒";
				if (second == 0)
					return minute + "分";
				return minute + "分" + second + "秒";
			}
			if ("answerRate".compareToIgnoreCase(column) == 0
					&& !"".equals(result)) {
				if (result.indexOf(".") > -1)
					result = result.substring(0, result.indexOf("."));
			}
			//System.out.println(result);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public String getDate(String tableName, String column, Long apId) {
		try {
			String sql = "select `" + column + "` from `" + tableName
					+ "` where activityPerson_id = " + apId;

			List<Object[]> list = new ArrayList<Object[]>();
			list = erService.getInfoBySql(sql);
			String result = "";
			if (list != null && list.size() > 0) {
				result = list.get(0)[0].toString();
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// 需要加toolid条件取值
	public String getMark(HashMap hApResultData, String tableName,
			String column, String module_id, Long apId, String pro) {
		try {

			// System.out.println("getMark......");

			HashMap<String, String> hApTableResultData = (HashMap<String, String>) hApResultData
					.get(tableName);
			if (hApTableResultData == null)// 第一次获取数据
			{
				hApTableResultData = getTableInfoByAPIDProID(tableName, apId,
						pro);
				hApResultData.put(tableName, hApTableResultData);
			}
			if (hApTableResultData.size() == 0)// 该用户没有数据，异常
				return "";

			String result = hApTableResultData.get(module_id + "_" + column);
			if (result == null || "".equals(result))// 缓存中没有找到数据
			{
				String sql = "select `" + column + "` from `" + tableName
						+ "` where module_id = " + module_id
						+ " and activityPerson_id = " + apId;
				String[] str = pro.split("_");
				if ("t".equals(str[0])) {
					sql += " and tool_id = " + str[1];
				}
				List<Object[]> list = erService.getInfoBySql(sql);
				result = "";
				if (list != null && list.size() > 0) {
					result = list.get(0)[0].toString();
				}
			}
			if (!"".equals(result)
					&& ("t_mark".equals(column) || "p_modified".equals(column))) {
				if (result.contains(".0")) {
					result = result.replace(".0", "");
				}
			} else if (!"".equals(result) && "avgDimeMark".equals(column)||"mark".equals(column)) { // 四舍五入取整
				result = (new BigDecimal(result).setScale(0,
						BigDecimal.ROUND_HALF_UP)).toString();
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String desc(String tableName, String column, String module_id,
			Long apId) {
		try {
			String sql = "select `" + column + "` from `" + tableName
					+ "` where module_id = " + module_id
					+ " and activityPerson_id = " + apId;
			List<Object[]> list = erService.getInfoBySql(sql);
			String result = "";
			if (list != null && list.size() > 0) {
				result = list.get(0)[0].toString();
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String getValid(Long apId) {
		String result = "有效";
		List<ActivityPersonTools> list = aptService.getByActivityPersonId(apId);
		if (list != null && list.size() > 0) {
			for (ActivityPersonTools bean : list) {
				if (bean.getValidity() == null
						|| !"有效".equals(bean.getValidity())) {
					result = "无效";
					break;
				}
			}
		}
		return result;
	}

	public String getLevel(String tableName, String condition,
			String module_id, String column, Long apId) throws Exception {
		try {
			// String sql =
			// "select `"+column+"` from s_dd_level where level = (select `"+condition
			// +"` from `"+tableName+"` where module_id = "+module_id+" and activityPerson_id = "+apId+")";

			// String sql = "SELECT a.`"+column+"` FROM s_dd_level a, "
			// +tableName+" b WHERE a.`"+condition+"` = b.`"+condition
			// +"` AND b.module_id = "+module_id+" AND b.activityPerson_id = "+apId;
			// List<Object[]> list = erService.getInfoBySql(sql);
			// String result = list.get(0)[0].toString();

			String sql = "select `" + condition + "` from `" + tableName
					+ "` where module_id = " + module_id
					+ " and activityPerson_id = " + apId;
			List<Object[]> list = erService.getInfoBySql(sql);
			String result2 = "";
			if (list != null && list.size() > 0) {
				result2 = list.get(0)[0].toString();
			}

			new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
																	// s_dd_level在主库
			String sql2 = "select `" + column + "` from s_dd_level where `"
					+ condition + "` = '" + result2 + "'";
			List<Object[]> list2 = erService.getInfoBySql(sql2);
			String result = "";
			if (list2 != null && list2.size() > 0) {
				result = list2.get(0)[0].toString();
			}

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			ActivityPersons bean = apService.getById(apId);
			String dbid = "";
			CustomerContextHolder cch = new CustomerContextHolder();
			dbid = String.valueOf(cch.getDbIdByseqCode(bean.getSeqCode()));
			if (!"1".equals(dbid)) {
				cch.setDynamicDataSource(dbid);
			}
		}
	}

	public String roleAdvantage(String tableName, String sorttoolid,
			String version, String sortcondition, String sortorder,
			String rank, String colum, String rows, Long apId) throws Exception {
		try {
			// new CustomerContextHolder().setDynamicDataSource("0"); //还原成主库
			// s_dd_level在主库
			// StringBuffer sql = new
			// StringBuffer("SELECT `"+colum+"` FROM `s_dd_role_advantage` WHERE 1=1");
			// sql.append(" and module_id = (SELECT a.module_id FROM `s_exam_advance_result` a,`"+tablename+"` b");
			// sql.append(" WHERE a.`module_id` = b.`module_id`");
			// if(sorttoolid!=null && !"".equals(sorttoolid)){
			// sql.append(" AND b.`tool_id` = "+sorttoolid);
			// }
			// int rank1 = Integer.parseInt(rank)-1;
			// sql.append(" AND b.`version` = "+version);
			// sql.append(" and a.activityPerson_id = "+apId);
			// sql.append(" ORDER BY a.`"+sortcondition+"` "+sortorder+" LIMIT "+rank1+",1) LIMIT 1");
			// List<Object[]> list = erService.getInfoBySql(sql.toString());
			// String result = list.get(0)[0].toString();

			new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
																	// s_dd_N在主库
			String sql1 = "select module_id from " + tableName
					+ " where `version` = " + version;
			if (sorttoolid != null && !"".equals(sorttoolid)) {
				sql1 += " and tool_id = " + sorttoolid;
			}
			List<Object[]> list1 = erService.getInfoBySql(sql1);
			String moduleIds = "";
			for (int i = 0; i < list1.size(); i++) {
				if (list1.get(i)[0] != null && !"".equals(list1.get(i)[0])) {
					moduleIds += list1.get(i)[0].toString() + ",";
				}
			}
			if (!"".equals(moduleIds)) {
				moduleIds = moduleIds.substring(0, moduleIds.length() - 1);
			} else {
				return null;
			}

			// 换成测评者作答结果所在的库
			ActivityPersons bean = apService.getById(apId);
			String dbid = "";
			CustomerContextHolder cch = new CustomerContextHolder();
			dbid = String.valueOf(cch.getDbIdByseqCode(bean.getSeqCode()));
			if (!"1".equals(dbid)) {
				cch.setDynamicDataSource(dbid);
			}
			int rank1 = Integer.parseInt(rank) - 1;
			String sql2 = "select module_id from `s_exam_advance_result` where 1=1";
			sql2 += " and module_id in (" + moduleIds + ")";
			sql2 += " and activityPerson_id = " + apId;
			sql2 += " ORDER BY `" + sortcondition + "` " + sortorder
					+ " LIMIT " + rank1 + ",1";
			List<Object[]> list2 = erService.getInfoBySql(sql2);
			String module_id = "";
			if (list2 != null && list2.size() > 0) {
				module_id = list2.get(0)[0].toString();
			} else {
				return null;
			}

			new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
																	// s_dd_level在主库
			String sql3 = "SELECT `" + colum
					+ "` FROM `s_dd_role_advantage` WHERE 1=1";
			sql3 += " and module_id = " + module_id;
			String result = "";
			if (!"all".equals(rows)) {
				int row = Integer.parseInt(rows) - 1;
				sql3 += " limit " + row + ",1";
				List<Object[]> list3 = erService.getInfoBySql(sql3);
				if (list3 != null && list3.size() > 0) {
					result += list3.get(0)[0].toString();
				}
			} else {
				List<Object[]> list3 = erService.getInfoBySql(sql3);
				for (int i = 0; i < list3.size(); i++) {
					if (list3.get(i)[0] == null || "".equals(list3.get(i)[0])) {
						break;
					} else {
						result += list3.get(i)[0] + ",";
					}
				}
				if (!"".equals(result)) {
					result = result.substring(0, result.length() - 1);
				}
			}

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			ActivityPersons bean = apService.getById(apId);
			String dbid = "";
			CustomerContextHolder cch = new CustomerContextHolder();
			dbid = String.valueOf(cch.getDbIdByseqCode(bean.getSeqCode()));
			if (!"1".equals(dbid)) {
				cch.setDynamicDataSource(dbid);
			}
		}
	}

	public String sortdisc(String tableName, String sorttoolid, String version,
			String sortcondition, String sortorder, String colum, Long apId,
			String old_dbid, String product) throws Exception {
		try {

			// //换成测评者作答结果所在的库
			// ActivityPersons bean = apService.getById(apId);
			// String dbid = "";
			// CustomerContextHolder cch = new CustomerContextHolder();
			// dbid = String.valueOf(cch.getDbIdByseqCode(bean.getSeqCode()));
			// if (!"1".equals(dbid)) {
			// cch.setDynamicDataSource(dbid);
			// }
			//
			// new CustomerContextHolder().setDynamicDataSource("0"); //还原成主库
			// s_dd_module_sort在主库
			// String sql1 =
			// "SELECT modulename FROM s_exam_advance_result WHERE activityperson_id="+
			// apId+" AND "+ sortcondition +" >60 AND module_id IN "+
			// "(SELECT module_id FROM s_dd_module_sort WHERE ";
			//
			String result = "";
			new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
																	// s_dd_module_sort在主库
			String sql1 = "SELECT module_id FROM s_dd_module_sort WHERE ";
			if (sorttoolid != null && !"".equals(sorttoolid))
				sql1 += "tool_id=" + sorttoolid + " AND ";
			sql1 += " VERSION=" + version;

			String[] str = product.split("_");
			if ("s".equals(str[0])) {
				sql1 += " and solution_id =" + str[1];
			}

			List<Object[]> list1 = erService.getObjListBySql(sql1, 1);
			String moduleIds = "";
			for (int i = 0; i < list1.size(); i++) {
				if (list1.get(i)[0] != null && !"".equals(list1.get(i)[0])) {
					if (moduleIds.length() == 0)
						moduleIds = (Long) list1.get(i)[0] + "";
					else
						moduleIds += "," + list1.get(i)[0];
				}
			}

			new CustomerContextHolder().setDynamicDataSource(old_dbid);
			String sql2 = "SELECT modulename FROM s_exam_advance_result WHERE activityperson_id="
					+ apId
					+ " AND "
					+ sortcondition
					+ " >60 AND module_id IN ("
					+ moduleIds
					+ ") ORDER BY "
					+ sortcondition + " " + sortorder;
			list1 = erService.getObjListBySql(sql2, 1);
			if (list1.size() == 0)// 如果分数都不大于60分，取最高的一个维度
			{
				sql2 = "SELECT modulename FROM s_exam_advance_result WHERE activityperson_id="
						+ apId
						+ "  AND module_id IN ("
						+ moduleIds
						+ ") ORDER BY "
						+ sortcondition
						+ " "
						+ sortorder
						+ " limit 1";
				list1 = erService.getObjListBySql(sql2, 1);
			}
			String discTypes = "";
			for (int i = 0; i < list1.size(); i++) {
				if (list1.get(i)[0] != null) {
					String tmpName = (String) list1.get(i)[0];
					if (tmpName.length() > 1) {
						String letter = tmpName.substring(1, 2);
						if (discTypes.indexOf(letter) == -1)
							discTypes += tmpName.substring(1, 2);
					}
				}
			}

			new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
																	// s_dd_module_sort在主库
			for (int i = discTypes.length(); i > 0; i--) {
				String type = discTypes.substring(0, i);
				String sql3 = "SELECT " + colum
						+ " FROM s_dd_disc WHERE TYPE='" + type + "'";
				list1 = erService.getObjListBySql(sql3, 1);
				if (list1.size() > 0) {
					result = (String) list1.get(0)[0];
					break;
				}
			}

			System.out.println(result);

			return result;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			CustomerContextHolder cch = new CustomerContextHolder();
			cch.setDynamicDataSource(old_dbid);
		}
	}

	public String sortModuleDesc(String tableName, String sorttoolid,
			String version, String sortcondition, String sortorder,
			String rank, String condition, String addtional_order_conditions,
			Long apId, String product) throws Exception {
		try {
			new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
																	// s_dd_module_sort在主库
			String sql1 = "select DISTINCT(m.module_id),m.moduletype from s_dd_module_sort ms,s_zhiding_module m where ms.version = "
					+ version + " and m.module_id=ms.module_id";
			if (sorttoolid != null && !"".equals(sorttoolid)) {
				sql1 += " and ms.tool_id = " + sorttoolid;
			}
			String[] str = product.split("_");
			if ("t".equals(str[0])) {
				sql1 += " and ms.tool_id=" + str[1];
			} else {
				sql1 += " and ms.solution_id =" + str[1];
			}
			String addtional_sort_conditions = "asc";
			if (sortorder.compareToIgnoreCase("asc") == 0)
				addtional_sort_conditions = "desc";
			if (addtional_order_conditions != null
					&& !"".equals(addtional_order_conditions))
				sql1 += " order by ms." + addtional_order_conditions + " "
						+ addtional_sort_conditions;
			List<Object[]> list1 = erService.getObjListBySql(sql1, 2);
			String moduleIds = "";
			boolean isLowLevel = false;// 是否是一级维度
			for (int i = 0; i < list1.size(); i++) {
				if (list1.get(i)[0] != null && !"".equals(list1.get(i)[0])) {
					moduleIds += list1.get(i)[0].toString() + ",";
				}
				if ("1".equals(list1.get(i)[1].toString()))
					isLowLevel = true;
			}
			if (!"".equals(moduleIds)) {
				moduleIds = moduleIds.substring(0, moduleIds.length() - 1);
			} else {
				return null;
			}

			System.out.println("xx");

			// 换成测评者作答结果所在的库
			ActivityPersons bean = apService.getById(apId);
			String dbid = "";
			CustomerContextHolder cch = new CustomerContextHolder();
			dbid = String.valueOf(cch.getDbIdByseqCode(bean.getSeqCode()));
			if (!"1".equals(dbid)) {
				cch.setDynamicDataSource(dbid);
			}
			int rank1 = Integer.parseInt(rank) - 1;

			String sql2 = "";
			if (!isLowLevel) {
				sql2 += "select module_id from `s_exam_advance_result` where 1=1";
			} else {
				sql2 += "select module_id from `s_exam_result` where 1=1";
			}
			sql2 += " and module_id in (" + moduleIds + ")";
			sql2 += " and activityPerson_id = " + apId;
			// sql2 += " ORDER BY `"+sortcondition+"` "+sortorder;
			// if( addtional_order_conditions != null &&
			// !"".equals(addtional_order_conditions) )
			sql2 += " ORDER BY `" + sortcondition + "` " + sortorder
					+ ", field(module_id," + moduleIds + ")  LIMIT " + rank1
					+ ",1";
			// else
			// sql2 += " ORDER BY `"+sortcondition+"` "+sortorder
			// +" LIMIT "+rank1+",1";

			List<Object[]> list2 = erService.getInfoBySql(sql2);
			String module_id = "";
			if (list2.size() > 0 && !"".equals(list2.get(0)[0])) {
				module_id += list2.get(0)[0].toString();
			} else {
				return "";
			}

			new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
																	// s_dd_module_desc在主库
			String sql3 = " select " + condition
					+ " from `s_dd_module_desc` where 1=1";
			// 这里处理的逻辑是如果取parent_name字段，判断函数的最后一个条件也就是parentid
			// 如果parentid没有值，那么将第二句sql找到的moduleid作为parentid用
			if ("parent_name".equals(condition)) {
				if (addtional_order_conditions != null
						&& !"".equals(addtional_order_conditions)) {
					sql3 += " and module_id = " + module_id;
					sql3 += " and parent_id = " + addtional_order_conditions;
				} else {
					sql3 += " and parent_id = " + module_id;
				}
			} else {
				sql3 += " and module_id = " + module_id;
				// // if(parentid!=null && !"".equals(parentid)){
				// // sql3 += " and parent_id = "+parentid;
				// }
			}
			String result = "";
			List<Object[]> list3 = erService.getInfoBySql(sql3);
			if (list3.size() > 0 && !"".equals(list3.get(0)[0])) {
				result = list3.get(0)[0].toString();
			}

			return result;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			ActivityPersons bean = apService.getById(apId);
			String dbid = "";
			CustomerContextHolder cch = new CustomerContextHolder();
			dbid = String.valueOf(cch.getDbIdByseqCode(bean.getSeqCode()));
			if (!"1".equals(dbid)) {
				cch.setDynamicDataSource(dbid);
			}
		}
	}

	/*
	 * public String sortModuleDesc(String tableName, String sorttoolid, String
	 * version, String sortcondition, String sortorder, String rank, String
	 * condition, String parentid, Long apId, String product) throws Exception{
	 * try { new CustomerContextHolder().setDynamicDataSource("0"); //还原成主库
	 * s_dd_module_sort在主库 String sql1 =
	 * "select DISTINCT(module_id) from s_dd_module_sort where `version` = "
	 * +version; if(sorttoolid!=null && !"".equals(sorttoolid)){ sql1 +=
	 * " and tool_id = "+sorttoolid; } String[] str = product.split("_");
	 * if("t".equals(str[0])){ sql1 += " and solution_id is null"; }else{ sql1
	 * += " and solution_id is not null"; } List<Object[]> list1 =
	 * erService.getInfoBySql(sql1); String moduleIds = ""; for(int i=0;
	 * i<list1.size(); i++){ if(list1.get(i)[0]!=null &&
	 * !"".equals(list1.get(i)[0])){ moduleIds += list1.get(i)[0].toString() +
	 * ","; } } if(!"".equals(moduleIds)){ moduleIds = moduleIds.substring(0,
	 * moduleIds.length()-1); }else{ return null; }
	 * 
	 * //换成测评者作答结果所在的库 ActivityPersons bean = apService.getById(apId); String
	 * dbid = ""; CustomerContextHolder cch = new CustomerContextHolder(); dbid
	 * =String.valueOf(cch.getDbIdByseqCode(bean.getSeqCode()));
	 * if(!"1".equals(dbid)){ cch.setDynamicDataSource(dbid); } int rank1 =
	 * Integer.parseInt(rank)-1;
	 * 
	 * String sql2 = ""; if("s_dd_module_sort".equals(tableName)){ sql2 +=
	 * "select module_id from `s_exam_advance_result` where 1=1"; }else{ sql2 +=
	 * "select module_id from `"+tableName+"` where 1=1"; } sql2 +=
	 * " and module_id in ("+moduleIds+")"; sql2 +=
	 * " and activityPerson_id = "+apId; sql2 +=
	 * " ORDER BY `"+sortcondition+"` "+sortorder+" LIMIT "+rank1+",1";
	 * 
	 * List<Object[]> list2 = erService.getInfoBySql(sql2); String module_id =
	 * ""; if (list2.size() > 0 && !"".equals(list2.get(0)[0])) { module_id +=
	 * list2.get(0)[0].toString(); }else{ return ""; }
	 * 
	 * new CustomerContextHolder().setDynamicDataSource("0"); //还原成主库
	 * s_dd_module_desc在主库 String sql3 =
	 * " select "+condition+" from `s_dd_module_desc` where 1=1";
	 * //这里处理的逻辑是如果取parent_name字段，判断函数的最后一个条件也就是parentid
	 * //如果parentid没有值，那么将第二句sql找到的moduleid作为parentid用
	 * if("parent_name".equals(condition)){ if(parentid!=null &&
	 * !"".equals(parentid)){ sql3 += " and module_id = "+module_id; sql3 +=
	 * " and parent_id = "+parentid; }else{ sql3 +=
	 * " and parent_id = "+module_id; } }else{ sql3 +=
	 * " and module_id = "+module_id; if(parentid!=null &&
	 * !"".equals(parentid)){ sql3 += " and parent_id = "+parentid; } } String
	 * result = ""; List<Object[]> list3 = erService.getInfoBySql(sql3); if
	 * (list3.size() > 0 && !"".equals(list3.get(0)[0])) { result =
	 * list3.get(0)[0].toString(); }
	 * 
	 * return result;
	 * 
	 * } catch (Exception e) { e.printStackTrace(); return null; } finally {
	 * ActivityPersons bean = apService.getById(apId); String dbid = "";
	 * CustomerContextHolder cch = new CustomerContextHolder(); dbid
	 * =String.valueOf(cch.getDbIdByseqCode(bean.getSeqCode()));
	 * if(!"1".equals(dbid)){ cch.setDynamicDataSource(dbid); } } }
	 */
	public String compare(String tableName, String column, String moduleId,
			String moduleName1, String moduleName2, Long apId) {
		try {
			String sql = "select `" + column + "` from " + tableName
					+ " where `module_id` = " + moduleId
					+ " and activityPerson_id =" + apId;
			List<Object[]> list = erService.getInfoBySql(sql);
			double mark = 0d;
			if (list.size() > 0 && !"".equals(list.get(0)[0])) {
				mark = Double.parseDouble(list.get(0)[0].toString());
			}
			StringBuffer result = new StringBuffer("");
			if (mark > 15 || mark < -15) {
				result.append("较偏向于");
				if (mark > 15) {
					result.append(moduleName1);
				} else {
					result.append(moduleName2);
				}
			} else {
				result.append("无明显偏向");
			}

			return result.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @title: PPF中职业倾向的文字描述</p>
	 * @Description: </p>
	 * @param tableName
	 * @param column
	 * @param moduleId
	 * @param moduleName1
	 * @param moduleName2
	 * @param apId
	 * @return
	 * @author baixf
	 * @date 2016年9月9日
	 */
	public String compare_ppf(HashMap hApResultData, String tableName,
			String column, String moduleId, String moduleName1,
			String moduleName2, Long apId, String pro) {
		try {

			String vratio = getMark(hApResultData, tableName, column, moduleId,
					apId, pro);
			if (vratio == null || "".equals(vratio))
				return null;
			double d_ratio = (new BigDecimal(vratio).setScale(0,
					BigDecimal.ROUND_HALF_UP)).doubleValue();

			// System.out.println( "PPF_vratio===="+ vratio);
			d_ratio = d_ratio / 100;

			String state = "";
			if (d_ratio > 0.617911422 || d_ratio < 0.382088578) {
				state = "明显偏向于：";
				if (d_ratio > 0.5)
					state += moduleName1;
				else
					state += moduleName2;
			} else if (d_ratio > 0.55 || d_ratio < 0.45) {
				state = "较偏向于：";
				if (d_ratio > 0.5)
					state += moduleName1;
				else
					state += moduleName2;
			} else {
				state = "无明显偏向";
			}
			return state;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String sort(String tablename, String sorttoolid,
			String sortcondition, String sortorder, String rank, String column,
			Long apId) {
		try {
			String sql = "select `" + column + "` from `" + tablename
					+ "` where 1=1";
			sql += " and tool_id = " + sorttoolid + " and activityPerson_id = "
					+ apId;
			sql += " order by `" + sortcondition + "` " + sortorder;
			if (tablename.compareToIgnoreCase("s_exam_result") == 0)// 按住键排序
				sql += ",d_result_id asc";
			if (tablename.compareToIgnoreCase("s_exam_advance_result") == 0)// 按住键排序
				sql += ",ar_result_id asc";
			int rank1 = Integer.parseInt(rank) - 1;
			sql += " limit " + rank1 + ",1";

			List<Object[]> list = erService.getInfoBySql(sql);
			String result = "";
			if (list != null && list.size() > 0) {
				result = list.get(0)[0].toString();
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String sortResult(String tableName, String toolId,
			String solutioniId, String column, Long apId, String product) {
		try {
			StringBuffer sql = new StringBuffer("select `" + column
					+ "` from `" + tableName + "` where 1=1");
			if (!"".equals(toolId)) {
				sql.append(" and `tool_id` = " + toolId);
			} else {
				String[] str = product.split("_");
				if ("t".equals(str[0])) {
					sql.append(" and `tool_id` = " + str[1]);
				}
			}
			sql.append(" and activityPerson_id = " + apId);

			List<Object[]> list = erService.getInfoBySql(sql.toString());
			String result = "";
			if (list != null && list.size() > 0) {
				result = list.get(0)[0].toString();
			}
			if (!"".equals(result) && "mark".equals(column)) { // 四舍五入取整
				result = (new BigDecimal(result).setScale(0,
						BigDecimal.ROUND_HALF_UP)).toString();
			} else if (!"".equals(result)
					&& ("t_mark".equals(column) || "p_modified".equals(column))) {
				if (result.contains(".0")) {
					result = result.replace(".0", "");
				}
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @title: 专门为专业题定制的函数，字段中是什么分数，就显示什么</p>
	 * @Description: </p>
	 * @param tableName
	 * @param toolId
	 * @param solutioniId
	 * @param column
	 * @param apId
	 * @param product
	 * @return
	 * @author baixf
	 * @date 2016年10月17日
	 */
	public String sortResult_zy(String tableName, String toolId,
			String solutioniId, String column, Long apId, String product) {
		try {
			StringBuffer sql = new StringBuffer("select `" + column
					+ "` from `" + tableName + "` where 1=1");
			if (!"".equals(toolId)) {
				sql.append(" and `tool_id` = " + toolId);
			} else {
				String[] str = product.split("_");
				if ("t".equals(str[0])) {
					sql.append(" and `tool_id` = " + str[1]);
				}
			}
			sql.append(" and activityPerson_id = " + apId);

			List<Object[]> list = erService.getInfoBySql(sql.toString());
			String result = "";
			if (list != null && list.size() > 0) {
				result = list.get(0)[0].toString();
			}
			if (result.endsWith(".0"))
				result = result.substring(0, result.length() - 2);

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @title: 根据用户ID (URL参数ZDYY_Result_ID
	 *         -无需指定)、维度ID(moduleid)和工具ID(tool_id)，查询tablename指定的分数表
	 * @Description: </p>
	 * @param tableName
	 * @param toolId
	 * @param moduleId
	 * @param column
	 * @param floatCalType
	 * @param apId
	 * @return
	 * @author baixf
	 * @date 2016年10月17日
	 */
	public String mscore(String tableName, String toolId, String moduleId,
			String column, String floatCalType, Long apId) {
		try {
			if( "2421".equals(moduleId) )
				System.out.println("breakpoint");
			StringBuffer sql = new StringBuffer("select `" + column
					+ "` from `" + tableName + "` where 1=1");
			if (!"".equals(toolId)) {
				sql.append(" and `tool_id` = " + toolId);
			}
			sql.append(" and activityPerson_id = " + apId);
			sql.append(" and module_id = " + moduleId);

			List<Object[]> list = erService.getInfoBySql(sql.toString());
			String result = "";
			if (list != null && list.size() > 0) {
				result = list.get(0)[0].toString();
			}
			if ("ceil".compareToIgnoreCase(floatCalType) == 0) {
				result = new Double(Math.ceil(new Double(result))).toString();
			}
			if ("floor".compareToIgnoreCase(floatCalType) == 0) {
				result = new Double(Math.floor(new Double(result))).toString();
			}
			if ("round".compareToIgnoreCase(floatCalType) == 0) {
				result = (new BigDecimal(result).setScale(0,
						BigDecimal.ROUND_HALF_UP)).toString();
			}

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// 行为风格预警
	public String Warning(String tableName, String column, String moduleId,
			Long apId) {
		try {
			StringBuffer sql = new StringBuffer("select `" + column
					+ "` from `" + tableName + "` where 1=1");
			sql.append(" and module_id = " + moduleId
					+ " and activityPerson_id = " + apId);
			List<Object[]> list = erService.getInfoBySql(sql.toString());
			double d = 0d;
			if (list != null && list.size() > 0) {
				d = Double.parseDouble(list.get(0)[0].toString());
			}
			StringBuffer result = new StringBuffer("");
			if (d >= 80d) {
				result.append("预警");
			} else {
				result.append("无预警");
			}
			return result.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String development(String tableName, String sorttoolid,
			String sortversion, String sortcondition, String sortorder,
			String rank, String rows, String dev_type, String dev_version,
			Long apId, String product) throws Exception {
		try {
			new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
																	// s_dd_module_sort在主库
			StringBuffer sql1 = new StringBuffer(
					"select module_id from s_dd_module_sort where `version` = "
							+ sortversion);
			if (sorttoolid != null && !"".equals(sorttoolid)) {
				sql1.append(" and tool_id = " + sorttoolid);
			}
			String[] str = product.split("_");
			if ("t".equals(str[0])) {
				sql1.append(" and solution_id is null");
			} else {
				if (sorttoolid == null || "".equals(sorttoolid)) {
					sql1.append(" and solution_id = " + str[1]);
				}
			}
			List<Object[]> list1 = erService.getInfoBySql(sql1.toString());
			String moduleIds = "";
			for (int i = 0; i < list1.size(); i++) {
				if (list1.get(i)[0] != null && !"".equals(list1.get(i)[0])) {
					moduleIds += list1.get(i)[0].toString() + ",";
				}
			}
			if (!"".equals(moduleIds)) {
				moduleIds = moduleIds.substring(0, moduleIds.length() - 1);
			} else {
				return null;
			}

			// 换成测评者作答结果所在的库
			ActivityPersons bean = apService.getById(apId);
			String dbid = "";
			CustomerContextHolder cch = new CustomerContextHolder();
			dbid = String.valueOf(cch.getDbIdByseqCode(bean.getSeqCode()));
			if (!"1".equals(dbid)) {
				cch.setDynamicDataSource(dbid);
			}
			int rank1 = Integer.parseInt(rank) - 1;

			StringBuffer sql2 = new StringBuffer(
					"select module_id from `s_exam_advance_result` where 1=1");
			sql2.append(" and module_id in (" + moduleIds + ")");
			sql2.append(" and activityPerson_id = " + apId);
			sql2.append(" ORDER BY `" + sortcondition + "` " + sortorder
					+ " LIMIT " + rank1 + ",1");

			List<Object[]> list2 = erService.getInfoBySql(sql2.toString());
			String module_id = "";
			if (list2.size() > 0 && !"".equals(list2.get(0)[0])) {
				module_id += list2.get(0)[0].toString();
			} else {
				return "";
			}

			new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
																	// s_dd_development在主库
			StringBuffer sql3 = new StringBuffer(
					" select `content` from `s_dd_development` where 1=1");
			sql3.append(" and `type` = '" + dev_type + "'");
			sql3.append(" and module_id = " + module_id);
			sql3.append(" and `version` = " + dev_version);

			String result = "";
			List<Object[]> list3 = erService.getInfoBySql(sql3.toString());
			if (list3.size() > 0 && !"".equals(list3.get(0)[0])) {
				result = list3.get(0)[0].toString();
			}

			return result;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			ActivityPersons bean = apService.getById(apId);
			String dbid = "";
			CustomerContextHolder cch = new CustomerContextHolder();
			dbid = String.valueOf(cch.getDbIdByseqCode(bean.getSeqCode()));
			if (!"1".equals(dbid)) {
				cch.setDynamicDataSource(dbid);
			}
		}
	}

	/**
	 * 
	 * @title:将列id转换成excel列字母 </p>
	 * @Description: </p>
	 * @param columnIndex
	 * @return
	 * @author baixf
	 * @date 2016年12月6日
	 */
	public String excelColIndexToStr(int columnIndex) {
		if (columnIndex <= 0) {
			return null;
		}
		String columnStr = "";
		columnIndex--;
		do {
			if (columnStr.length() > 0) {
				columnIndex--;
			}
			columnStr = ((char) (columnIndex % 26 + (int) 'A')) + columnStr;
			columnIndex = (int) ((columnIndex - columnIndex % 26) / 26);
		} while (columnIndex > 0);
		return columnStr;
	}

	public static void main(String args[]) {
		// String str = "%#SINGLEDATA(tablename,column)#%";
		// analyzeFunc(str,0L);
		String str = "45.42958746";
		double d = Double.valueOf(str);
		double c = Double.parseDouble(str);
		System.out.println("1:" + d);
		System.out.println("2:" + c);

		System.out.println("四舍五入取整:(42.8571428571429)="
				+ new BigDecimal("42.8571428571429").setScale(0,
						BigDecimal.ROUND_HALF_UP));
		System.out.println("四舍五入取整:(78.5714285714286)="
				+ new BigDecimal("78.5714285714286").setScale(0,
						BigDecimal.ROUND_HALF_UP));
		System.out.println("四舍五入取整:(21.4285714285714)="
				+ new BigDecimal("21.4285714285714").setScale(0,
						BigDecimal.ROUND_HALF_UP));
		System.out.println("四舍五入取整:(64.2857142857143)="
				+ new BigDecimal("64.2857142857143").setScale(0,
						BigDecimal.ROUND_HALF_UP));
		System.out.println("四舍五入取整:(50)="
				+ new BigDecimal("50").setScale(0, BigDecimal.ROUND_HALF_UP));
		System.out.println("四舍五入取整:(str)="
				+ new BigDecimal(str).setScale(0, BigDecimal.ROUND_HALF_UP));

		String value = "-99.4285714285714";

		try {
			double intValue = Double.valueOf(value);

			if (intValue < 5)
				value = "5";
			if (intValue > 99)
				value = "99";
		} catch (Exception ex) {
			System.out.println(ex);
		}
		System.out.println(value);

	}

	/**
	 * 处理分数的极值问题，分数不能低于5分，大于99分
	 * 
	 * @Type:ExportUtil.java </p>
	 * @Description: </p>
	 * @param value
	 * @return
	 * @author baixf
	 * @date 2017年2月24日 下午2:45:58
	 */
	private String checkExtremeValue(String value) {
		try {
			double intValue = Double.valueOf(value);

			if (intValue < 5)
				value = "5";
			if (intValue > 99)
				value = "99";
		} catch (Exception ex) {
			System.out.println(ex);
		}
		return value;
	}

	public static int getDBNum() {
		int dbNum = -1;
		// 读取配置文件
		URL report_config = new ExportUtil().getClass().getClassLoader()
				.getResource("db.properties");
		InputStream in_report_config;
		try {
			in_report_config = report_config.openStream();
			Properties p_report_config = new Properties();
			p_report_config.load(in_report_config);
			String DBnum = p_report_config.getProperty("DBApartNum");
			dbNum = Integer.parseInt(DBnum);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("获取数据库分库信息失败");
			return -1;
		}
		return dbNum;
	}

}
