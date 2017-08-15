package cn.com.maxtech.activity.struts.action;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fins.gt.server.GridServerHandler;


import cn.com.job51.activity.model.SActivityNorm;
import cn.com.job51.activity.service.SActivityNormService;
import cn.com.job51.dynamicsetdb.CustomerContextHolder;
import cn.com.job51.relation.model.ActivityPersonTools;
import cn.com.job51.relation.service.ActivityPersonToolsService;
import cn.com.maxtech.activity.model.Activity;
import cn.com.maxtech.activity.model.ActivityPersons;
import cn.com.maxtech.activity.model.ActivityTools;
import cn.com.maxtech.activity.model.ExamResult;
import cn.com.maxtech.activity.struts.form.Product;
import cn.com.maxtech.activity.service.ActivityService;
import cn.com.maxtech.activity.service.ActivityToolService;
import cn.com.maxtech.activity.service.ProductsPersonsService;
import cn.com.maxtech.activity.util.MemcacheExport;
import cn.com.maxtech.common.action.CommonAction;
import cn.com.maxtech.common.memcached.MemcachedUtil;
import cn.com.maxtech.common.util.OSUtil;
import cn.com.maxtech.common.util.PropertiesUtil;
import cn.com.maxtech.enterprise.model.Enterprise;
import cn.com.maxtech.enterprise.model.EnterpriseHistory;
import cn.com.maxtech.enterprise.model.EnterpriseTools;
import cn.com.maxtech.enterprise.service.EnterpriseService;
import cn.com.maxtech.enterprise.service.EnterpriseToolsService;
import cn.com.maxtech.enterprise.service.impl.EnterpriseServiceImpl;
import cn.com.maxtech.enterprise.struts.action.HrLoginAction;
import cn.com.maxtech.enterprise.struts.form.EnterpriseToolsForm;
import cn.com.maxtech.exam.util.ExamPaperWithMemery;
import cn.com.maxtech.report.util.ReportBase;
import cn.com.maxtech.solutions.model.SolutionsTools;
import cn.com.maxtech.solutions.model.SolutionsZd;
import cn.com.maxtech.solutions.service.SolutionsToolsService;
import cn.com.maxtech.solutions.service.SolutionsZdService;
import cn.com.maxtech.solutions.util.SolutionUtil;
import cn.com.maxtech.tools.model.Tools;
import cn.com.maxtech.tools.model.ToolsNorm;
import cn.com.maxtech.tools.service.ToolsNormService;
import cn.com.maxtech.tools.service.ToolsService;
import cn.com.maxtech.tools.util.ExportUtil;
import cn.com.maxtech.tools.util.ReportJobUtil;
import cn.com.maxtech.util.Maxtech;
import cn.com.maxtech.util.ParamUtil;
import cn.com.zhiding.template.model.TReportTemplateEnActToolOrSol;
import cn.com.zhiding.template.service.TReportTemplateEnActToolOrSolService;
import jxl.Cell;
import jxl.FormulaCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.Formula;
import jxl.write.Label;
import jxl.write.NumberFormat;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

public class ActivityToolAction extends CommonAction {
	Logger logger = LoggerFactory.getLogger(ExportUtil.class);
	// private int cur_count = 0;//当前导出条数
	int status_count = 0;// 显示企业HR分数导出状态

	/**
	 * 转到活动的工具页面
	 * 
	 * @Title: ActivityToolAction
	 * @Description: TODO
	 */
	public ActionForward toActivityTools(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			SolutionsToolsService stService = (SolutionsToolsService) Maxtech
					.getInstance().getBean(SolutionsToolsService.ID_NAME);

			Long activityId = ParamUtil.getLongParamter(request, "activityId",
					0);

			Enterprise sessionEnterprise = getSessionEnterprise(request);

			if (activityId == 0 || sessionEnterprise == null)
				return null;

			ActivityService as = getActivityService();

			Activity bean = as.getById(activityId);

			request.setAttribute("bean", bean);
			if (bean.getActivityPersons().isEmpty()) {
				request.setAttribute("beanstate", 0);
			} else {
				request.setAttribute("beanstate", 1);
			}
			ActivityTools person = new ActivityTools();

			person.setEnterprise_id(sessionEnterprise.getId());
			person.setActivity_id(activityId);

			List<ActivityTools> listTools = getActivityToolService().find(
					person, null);
			List<ToolsNorm> listNorm = null;

			List<SolutionsZd> solutionList = getActivityToolService()
					.findSolutionsByActivityId(activityId);
			List<Integer> solutionTimeList = new ArrayList<Integer>();
			for (SolutionsZd sol : solutionList) {
				List<SolutionsTools> stlist = stService.getBySolutionId(sol
						.getId());
				int time = 0; // 方案的测试时间
				for (SolutionsTools st : stlist) {
					time += st.getTools().getTimeExam();
				}
				solutionTimeList.add(time);
			}

			request.setAttribute("solutionList", solutionList);
			request.setAttribute("solutionTime", solutionTimeList);
			// 以下为查询已购买工具中的常模
			StringBuffer tIds = new StringBuffer();
			if (listTools != null && listTools.size() > 0) {
				List<ActivityTools> noSoltionToolsList = new ArrayList<ActivityTools>();
				for (ActivityTools at : listTools) {
					if (at.getSolutionsZd() == null) {
						noSoltionToolsList.add(at);
						if (StringUtils.isNotBlank(tIds.toString()))
							tIds.append(",");
						tIds.append(at.getTools().getId());
					}
				}
				listTools = noSoltionToolsList;
			}

			if (StringUtils.isNotBlank(tIds.toString())) {
				// 首先根据活动中的工具查询出是那几个购买的工具
				EnterpriseToolsForm etf = new EnterpriseToolsForm();
				etf.setEnterpriseId(sessionEnterprise.getId());
				etf.setToolIds(tIds.toString());
				List<EnterpriseTools> listet = getEnterpriseToolsService()
						.find(etf, null);

				// 然后根据购买的工具查询他们的常模
				StringBuffer normIds = new StringBuffer();
				if (listet != null && listet.size() > 0) {
					for (EnterpriseTools et : listet) {
						if (StringUtils.isNotBlank(normIds.toString())) {
							normIds.append(",");
						}
						normIds.append(et.getReportNormsIds());
					}
				}

				ToolsNormService tns = (ToolsNormService) super
						.getFacotryBean(ToolsNormService.ID_NAME);

				listNorm = tns.findByIds(normIds.toString());

			}

			if (listNorm != null && listNorm.size() > 0) {
				for (ActivityTools at : listTools) {
					for (ToolsNorm tn : listNorm) {
						if (tn.getTools().getId().equals(at.getTools().getId())) {
							List<ToolsNorm> list = at.getHaveNorms();
							if (list != null) {
								list.add(tn);
								at.setHaveNorms(list);
							} else {
								list = new ArrayList<ToolsNorm>();
								list.add(tn);
								at.setHaveNorms(list);
							}
						}
					}

				}
			}

			request.setAttribute("listTools", listTools);

			String _frid = ParamUtil.getStr(request, "_frid", "");

			request.setAttribute("_frid", _frid);

			return mapping.findForward(SUCCESS);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * 为某活动下某个工具的所有测评人员设置随机题目顺序。（已随机不再生成）
	 * 
	 * @Title: ActivityToolAction
	 * @Description: TODO
	 */
	/**
     */
	public ActionForward randomQuestionOrder(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			Long activityId = ParamUtil.getLongParamter(request, "activityId",
					0);// 获取活动ID
			if (activityId <= 0) {
				super.printErrorJson(response, "查找活动出错！" + activityId);
				return null;
			}
			String toolsIds = ParamUtil.getStr(request, "toolsIds", "");// 获取工具列表ID
			if ("".equals(toolsIds)) {
				super.printErrorJson(response, "查找工具出错！" + toolsIds);
				return null;
			}
			String[] toolsarr = toolsIds.split(",");
			List<ActivityPersons> aps = getActivityPersonService()
					.findApsByActivityId(activityId);// 获取活动下所有人员
			for (String string : toolsarr) {// 循环所有工具
				if (string == null || "".equals(string)) {
					continue;
				}
				Long toolId = Long.parseLong(string);
				ExamPaperWithMemery epwm = new ExamPaperWithMemery();// 获取单例
				String paper[] = epwm.getPaperXmlFile(activityId, toolId);
				List<String> indexList = new ArrayList<String>();// 存放所有questionID
				String[] indexs = paper[1].split("_");// 获取所有题目ID，并分割
				for (String index : indexs) {
					if (index != null && !"".equals(index)) {
						indexList.add(index);// 添加到list
					}
				}
				for (ActivityPersons ap : aps) {// 循环测评者
					ExamResult er = getExamResultService()
							.findByActivityPersonAndTools(ap.getId(), toolId);// 取得该活动人该工具的EXAMRESULT
					if (er.getReportPdfPath() != null
							&& !"".equals(er.getReportPdfPath())) {// 检测是否存在已随机的题目顺序
						Collections.shuffle(indexList);// 打乱题目顺序
						StringBuffer sb = new StringBuffer();// 存放新的顺序
						for (String qid : indexList) {
							sb.append(qid).append("_");
						}
						er.setReportPdfPath(sb.toString());// 保存进改条顺序
						getExamResultService().saveOrUpdate(er);// 同步到数据库
					}
				}
			}

			return mapping.findForward(SUCCESS);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * 树形工具分类
	 */
	public ActionForward getToolsClassifyToJson(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		Long activityId = ParamUtil.getLongParamter(request, "activityId", 0);

		Enterprise enterprise = getSessionEnterprise(request);

		if (enterprise == null || activityId == 0)
			return null;

		// 查询已经添加的工具
		ActivityTools atool = new ActivityTools();
		atool.setActivity_id(activityId);
		atool.setEnterprise_id(enterprise.getId());
		List<ActivityTools> listat = getActivityToolService().find(atool, null);

		StringBuffer atIds = new StringBuffer();// 存放已经添加工具的id
		if (listat != null && listat.size() > 0) {
			for (ActivityTools at : listat) {
				if (StringUtils.isNotBlank(atIds.toString()))
					atIds.append(",");
				atIds.append(at.getTools().getId());
			}
		}
		EnterpriseToolsService es = getEnterpriseToolsService();
		List beans = es.findToolsType(enterprise.getId(), atIds.toString());
		super.printBeansJson(response, beans);

		return null;
	}

	/**
	 * 树形工具
	 */
	public ActionForward getToolsByClassifyIdToJson(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		Long classifyId = ParamUtil.getLongParamter(request, "classifyId", 0);
		Long activityId = ParamUtil.getLongParamter(request, "activityId", 0);
		if (classifyId == 0 || activityId == 0)
			return null;

		Enterprise enterprise = getSessionEnterprise(request);

		// 查询已经添加的工具
		ActivityTools atool = new ActivityTools();
		atool.setActivity_id(activityId);
		atool.setEnterprise_id(enterprise.getId());
		List<ActivityTools> listat = getActivityToolService().find(atool, null);

		StringBuffer atIds = new StringBuffer();// 存放已经添加工具的id
		if (listat != null && listat.size() > 0) {
			for (ActivityTools at : listat) {
				if (at.getSolutionsZd() == null) {
					if (StringUtils.isNotBlank(atIds.toString()))
						atIds.append(",");
					atIds.append(at.getTools().getId());
				}
			}
		}

		EnterpriseToolsService es = getEnterpriseToolsService();

		List beans = es.getBoughtToolsByClassifyId(classifyId,
				enterprise.getId(), atIds.toString());

		super.printBeansJson(response, beans);
		return null;
	}

	/**
	 * 从购买的工具中往活动工具中添加工具
	 * 
	 * @Title: ActivityToolAction
	 * @Description: TODO
	 */
	public ActionForward addActivityTool(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			Enterprise sessionEnterprise = getSessionEnterprise(request);

			Long activityId = ParamUtil.getLongParamter(request, "activityId",
					0);

			String cs = ParamUtil.getStr(request, "cs", "");

			List<EnterpriseTools> EnterpriseToolsList = getEnterpriseToolsService()
					.findByIds(cs.replace(";", ","));
			String toolsid = "";
			for (EnterpriseTools et : EnterpriseToolsList) {
				toolsid += et.getTools().getId() + ",";
			}
			toolsid = toolsid.substring(0, toolsid.length() - 1);
			// System.out.println("待判断方案的工具Str===="+toolsid);

			if (activityId == 0 || StringUtils.isBlank(cs))
				return null;

			EnterpriseToolsService ets = getEnterpriseToolsService();

			ActivityToolService as = getActivityToolService();

			String[] ids = cs.split(";");
			StringBuffer toolIds = new StringBuffer();

			if (ids != null && ids.length > 0) {
				for (String str : ids) {
					if (StringUtils.isNotBlank(toolIds.toString()))
						toolIds.append(",");
					toolIds.append(str);
				}
			}

			List<EnterpriseTools> listET = ets.findByIds(toolIds.toString());

			Activity activity = new Activity();
			activity.setId(activityId);

			// 开始生成试卷xml准备工作——yr
			// ActivityPersons ap = new ActivityPersons();
			// ap.setActivity(activity);
			// ap.setId(0l);
			// ap.setEnterprise(sessionEnterprise);
			// ap.setSeqCode("模拟ap");
			// ap.setPersonName("模拟ap");
			// String osPath = request.getSession().getServletContext()
			// .getRealPath(File.separator+"exam"+File.separator+"paperXml");
			// File filepath = new File(osPath);
			// filepath.mkdirs();
			// 结束生成试卷xml准备工作——yr

			// 获取工具名称及id
			String activity_tools_name = "";
			String activity_tools_id = "";

			SActivityNormService sactivityNormService = (SActivityNormService) Maxtech
					.getInstance().getBean(SActivityNormService.ID_NAME);
			for (EnterpriseTools et : listET) {
				ActivityTools at = new ActivityTools();
				at.setActivity(activity);
				at.setTools(et.getTools());
				at.setEnterprise(sessionEnterprise);
				at.setId(null);
				as.saveOrUpdate(at);

				// add by zw 插入s_activity_norm
				/*
				 * add by baixf s_activity_norm表变为定制表，通用情况不插入 SActivityNorm
				 * sactnorm = new SActivityNorm(); sactnorm.setNormVersion("1");
				 * sactnorm.setToolId(et.getTools().getId());
				 * sactnorm.setActivityId(activityId);
				 * sactivityNormService.saveOrUpdate(sactnorm);
				 */
				activity_tools_id += at.getTools().getId() + ",";
				activity_tools_name += at.getTools().getToolName() + ",";
				// 开始生成试卷xml——yr
				// String fileName =
				// osPath+File.separator+at.getActivity().getId()+"_"+at.getTools().getId()+".xml";
				// File file = new File(fileName);
				// if(file.exists()){
				// System.out.println("已存在试卷xml文件(acitivityID_toolsID):"+fileName);
				// continue;
				// }else{
				// String xml = ToolsUtil.ToolsToXml(et.getTools(), ap);//
				// 生成考试试卷的xml
				// System.out.println("生成试卷xml文件(acitivityID_toolsID):"+fileName);
				// OutputStreamWriter osw = new OutputStreamWriter(new
				// FileOutputStream(fileName, true),"UTF-8");
				// osw.write(xml);
				// osw.flush();
				// osw.close();
				//
				// while(!file.renameTo(file)){//判断文件是否生成完
				// try {
				// Thread.sleep(50);//50ms判断一次
				// } catch (InterruptedException e) {
				// e.printStackTrace();
				// }
				// }
				// }
				// 结束生成试卷xml——yr
			}

			// 调用业务操作日志属性文件
			String pro_path = request.getSession().getServletContext()
					.getRealPath("/WEB-INF/classes");
			PropertiesUtil pro = new PropertiesUtil(pro_path
					+ "/EnterpriseHistorylog.properties");
			// 添加服务记录
			EnterpriseHistory enterHis = new EnterpriseHistory();
			enterHis.setId(null);
			enterHis.setServiceDate(new Date());
			enterHis.setOperBusiness(pro.getValue("oper_business").split(",")[2]);
			enterHis.setOperType(pro.getValue("oper_type").split(",")[3]);
			enterHis.setEnterprise(sessionEnterprise);
			enterHis.setOperManId(HrLoginAction.getUser(request).getId());
			enterHis.setActivity(activity);
			enterHis.setActivityTools(activity_tools_id.substring(0,
					activity_tools_id.length() - 1));
			enterHis.setRemark("配置活动工具（工具名称为 "
					+ activity_tools_name.substring(0,
							activity_tools_name.length() - 1) + "）");
			enterHis.setOperNum(activity_tools_id.split(",").length);
			getEnterpriseHistoryService().saveOrUpdate(enterHis);

			super.printSuccessJson(response, "操作成功");

			return null;
		} catch (Exception e) {
			e.printStackTrace();
			super.printErrorJson(response, "操作失败，服务器出错");

			return null;
		}

	}

	/**
	 * 删除已经添加的工具
	 * 
	 * @Title: ActivityToolAction
	 * @Description: TODO
	 */
	public ActionForward removeActivityTool(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			Enterprise sessionEnterprise = getSessionEnterprise(request);

			Long toolId = ParamUtil.getLongParamter(request, "toolId", 0);

			if (toolId == 0)
				return null;

			// add by zw 删除s_activity_norm数据
			List<ActivityTools> acttoolList = getActivityToolService()
					.findByIds(toolId.toString());
			ActivityTools activityTools = (ActivityTools) acttoolList.get(0);
			/*
			 * add by baixf s_activity_norm表变为定制表，通用情况不插入 SActivityNormService
			 * sactivityNormService =
			 * (SActivityNormService)Maxtech.getInstance()
			 * .getBean(SActivityNormService.ID_NAME); SActivityNorm sactnorm =
			 * new SActivityNorm();
			 * sactnorm.setToolId(activityTools.getTools().getId());
			 * sactnorm.setActivityId(activityTools.getActivity().getId());
			 * List<SActivityNorm> sActivityNormList =
			 * sactivityNormService.findByExample(sactnorm); SActivityNorm
			 * asctNorm = (SActivityNorm)sActivityNormList.get(0);
			 * sactivityNormService.remove(asctNorm.getId());
			 */
			getActivityToolService().deleteByIds(toolId.toString(),
					sessionEnterprise.getId());
			super.printSuccessJson(response, "操作成功");

			return null;
		} catch (Exception e) {
			e.printStackTrace();
			super.printErrorJson(response, "操作失败，服务器出错");

			return null;
		}

	}

	/**
	 * 保存更新活动中的工具的常模
	 * 
	 * @Title: ActivityToolAction
	 * @Description: TODO
	 */
	public ActionForward saveActivityTool(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			Long activityId = ParamUtil.getLongParamter(request, "activityId",
					0L);

			String toolsIds = "";

			String tids = ParamUtil.getStr(request, "toolItems", "");

			String sId = ParamUtil.getStr(request, "solutionId", "");

			if (StringUtils.isBlank(tids) && StringUtils.isBlank(sId)) {
				return null;
			}

			ActivityToolService ats = getActivityToolService();

			if (!StringUtils.isBlank(tids)) { // 活动配置工具的情况

				List<ActivityTools> listat = ats.findByIds(tids);

				// add by zw on 20150925
				// 判断所选工具串是否为一个方案，如果是则插入ActivityTool对象时更新solution_id
				// String tools_id = "";
				// for(ActivityTools at : listat) {
				// tools_id+=at.getTools().getId()+",";
				// }
				// SolutionsZd szd =
				// SolutionUtil.isSolution(tools_id.substring(0,tools_id.length()-1));
				// boolean isSoution = false;
				// if(szd !=null){
				// isSoution = true;
				// }

				if (listat != null && listat.size() > 0) {
					for (ActivityTools at : listat) {
						Long id = at.getId();
						Long normId = ParamUtil.getLongParamter(request,
								"toolsNorm_" + id, 0);
						if (normId == 0) {// 没有常模的情况
							// if(isSoution) at.setSolutionsZd(szd);
						} else {
							ToolsNorm tn = new ToolsNorm();
							tn.setId(normId);
							at.setToolsNorm(tn);
							// add by zw on20150401 设置方案id
							// if(isSoution) at.setSolutionsZd(szd);
						}
						ats.saveOrUpdate(at);

						toolsIds += at.getTools().getId() + ",";

					}
				}
			}

			// 填写activity_tools表的products和customized字段
			if (activityId != null && activityId > 0) {
				int proNum = 0;
				proNum = ReportJobUtil.getProductNum(activityId);
				String products = "";
				if (proNum > 1) { // 是多产品
					if (!"".equals(toolsIds)) {
						toolsIds = toolsIds.substring(0, toolsIds.length() - 1);
						String[] temp = toolsIds.split(",");
						for (int i = 0; i < temp.length; i++) {
							if (i == temp.length - 1) {
								products += "t_" + temp[i];
							} else {
								products += "t_" + temp[i] + ",";
							}
						}
					}
					if (!"".equals(sId)) {
						if (!"".equals(products)) {
							products += ",";
						}
						String[] temp = sId.split(",");
						for (int j = 0; j < temp.length; j++) {
							if (j == temp.length - 1) {
								products += "s_" + temp[j];
							} else {
								products += "s_" + temp[j] + ",";
							}
						}
					}
					if (!"".equals(products)) {
						List<ActivityTools> atList = ats
								.findByActivityid(activityId);

						for (ActivityTools at : atList) {
							at.setProducts(products);
							at.setCustomized("1");
							ats.saveOrUpdate(at);
						}
					}
				} else { // 不是多产品的活动
					List<ActivityTools> atList = ats
							.findByActivityid(activityId);

					for (ActivityTools at : atList) {
						at.setProducts(null);
						at.setCustomized("0");
						ats.saveOrUpdate(at);
					}
				}
			}

			super.printSuccessJson(response, "操作成功");

			return null;
		} catch (Exception e) {
			e.printStackTrace();
			super.printErrorJson(response, "操作失败，服务器出错");

			return null;
		}

	}

	/**
	 * 系统推荐工具
	 * 
	 * @Description: TODO
	 */
	public ActionForward addSystemActivityTool(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			Enterprise sessionEnterprise = getSessionEnterprise(request);

			Long activityId = ParamUtil.getLongParamter(request, "activityId",
					0);

			if (activityId == 0 || sessionEnterprise == null)
				return null;

			response.setCharacterEncoding("utf-8");

			PrintWriter out = response.getWriter();

			EnterpriseToolsService ets = getEnterpriseToolsService();

			ActivityToolService as = getActivityToolService();

			Activity activity = getActivityService().getById(activityId);

			EnterpriseToolsForm etf = new EnterpriseToolsForm();
			etf.setEnterpriseId(sessionEnterprise.getId());

			// 活动中的职位序列可以为空，所以在这里需要判断
			if (activity.getJobSeq_1() != null
					&& activity.getJobSeq_2() != null
					&& activity.getJobSeq_3() != null) {
				etf.setJobseq_id(activity.getJobSeq_3().getId());
			} else if (activity.getJobSeq_1() != null
					&& activity.getJobSeq_2() != null
					&& activity.getJobSeq_3() == null) {
				etf.setJobseq_id_2(activity.getJobSeq_2().getId());
			} else if (activity.getJobSeq_1() != null
					&& activity.getJobSeq_2() == null
					&& activity.getJobSeq_3() == null) {
				etf.setJobseq_id_1(activity.getJobSeq_1().getId());
			} else {
				String data = "{status:1,message:'"
						+ ParamUtil.toJsString("没有系统推荐的工具") + "',type:1}";

				out.print(data);
				return null;
			}
			//ny 20170801  去掉工具的服务期限时间
			//etf.setServiceDate(new Date());

			List<EnterpriseTools> listET = ets.find(etf, null);

			if (listET == null || listET.size() <= 0) {
				String data = "{status:1,message:'"
						+ ParamUtil.toJsString("没有系统推荐的工具") + "',type:1}";

				out.print(data);
				return null;
			}

			as.deleteByActivity(activityId, sessionEnterprise.getId());

			for (EnterpriseTools et : listET) {

				ActivityTools at = new ActivityTools();
				at.setActivity(activity);
				at.setTools(et.getTools());
				at.setEnterprise(sessionEnterprise);
				at.setId(null);

				as.saveOrUpdate(at);
			}

			super.printSuccessJson(response, "操作成功");

			return null;
		} catch (Exception e) {
			e.printStackTrace();
			super.printErrorJson(response, "操作失败，服务器出错");

			return null;
		}

	}

	public ActionForward toActivityNotice(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			Enterprise sessionEnterprise = getSessionEnterprise(request);

			Long activityId = ParamUtil.getLongParamter(request, "activityId",
					0);

			if (activityId == 0 || sessionEnterprise == null)
				return null;

			Activity bean = getActivityService().getById(activityId);

			String _frid = ParamUtil.getStr(request, "_frid", "");

			request.setAttribute("_frid", _frid);

			request.setAttribute("bean", bean);

			return mapping.findForward(SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			super.printErrorJson(response, "操作失败，服务器出错");

			return null;
		}

	}

	public ActionForward saveActivityNotice(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			Enterprise sessionEnterprise = getSessionEnterprise(request);

			Long activityId = ParamUtil.getLongParamter(request, "activityId",
					0);

			if (activityId == 0 || sessionEnterprise == null)
				return null;

			String notice = ParamUtil.getStr(request, "notice", "");

			Activity bean = getActivityService().getById(activityId);

			bean.setNotice(notice);

			getActivityService().saveOrUpdate(bean);

			super.printSuccessJson(response, "操作成功");

			return null;
		} catch (Exception e) {
			e.printStackTrace();
			super.printErrorJson(response, "操作失败，服务器出错");

			return null;
		}

	}
		public  ActionForward getAjax(ActionMapping mapping, ActionForm form,
				HttpServletRequest request, HttpServletResponse response)throws Exception{
			logger.info("------------->>getAjax");
		String product = request.getParameter("value");// t_376
		String enidStr = request.getParameter("eid");// 34077
		String[] pro = product.split("_");
		Long p_id = Long.parseLong(pro[1]);
		Long enid = Long.parseLong(enidStr);
		System.out.println("value---->"+product);
		System.out.println("enid---->"+enid);
		Map map = new HashMap();
		JSONArray jsonArray = new JSONArray();
		List<Product> list = new ArrayList<Product>();
		List<Product> reportIdAndName = new ArrayList<Product>();
		TReportTemplateEnActToolOrSolService templateEnActToolOrSolService = (TReportTemplateEnActToolOrSolService) Maxtech.getInstance().getBean(TReportTemplateEnActToolOrSolService.ID_NAME);
		if ("t".equals(pro[0])) {
			List<TReportTemplateEnActToolOrSol> actToolOrSols = new ArrayList<TReportTemplateEnActToolOrSol>();
			actToolOrSols = templateEnActToolOrSolService.findByEnterpriceAndToolsLinkage(enid, p_id);
		System.out.println(actToolOrSols.size());
		if(actToolOrSols.isEmpty()||actToolOrSols==null){
			map.put("flag", false);
		}
			/*if(!actToolOrSols.isEmpty()&&actToolOrSols!=null){*/
			for(TReportTemplateEnActToolOrSol et:actToolOrSols){
				
				Product reportId = new Product();
				reportId = new Product();
				reportId.settOrSId(et.getReportTemplate().getId());
				reportId.setName(et.getReportTemplate().getName());
				list.add(reportId);
				map.put("list", list);
				
			}/*}*/
		request.setAttribute("reportIdAndName", list);
		jsonArray.add(map);
		System.out.println(jsonArray.toString());
		super.printBeansJson(response,jsonArray);
		return null; 
		
		} else {
			List<TReportTemplateEnActToolOrSol> actToolOrSols = new ArrayList<TReportTemplateEnActToolOrSol>();
			actToolOrSols = templateEnActToolOrSolService.findByEnterpriceAndSolutionLinkage(enid, p_id);
			if(actToolOrSols.isEmpty()||actToolOrSols==null){
				map.put("flag", false);
			}
			/*if(!actToolOrSols.isEmpty()&&actToolOrSols!=null){*/
				Product reportId = null;
				for(TReportTemplateEnActToolOrSol et:actToolOrSols){
					reportId = new Product();
					reportId.settOrSId(et.getReportTemplate().getId());
					reportId.setName(et.getReportTemplate().getName());
					list.add(reportId);
					map.put("list", list);
				}/*}*/
			jsonArray.add(map);
			super.printBeansJson(response, jsonArray);
			logger.info(jsonArray.toString());
			return null;
		}
		}
		
	public ActionForward getProducts(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		HttpSession session = request.getSession();
		session.removeAttribute("TOTALCOUNT");
		session.removeAttribute("CURRCOUNT");
		session.removeAttribute("STARTTIME");
		this.status_count = ExportUtil.EXPORTSCORE_INIT;
		logger.info("------------->>getProducts");
		try {
			Long activityId = ParamUtil.getLongParamter(request, "activityId",
					0L);
			if (activityId == 0L) {
				printErrorJson(response, "参数错误！");
				return null;
			}

			List<Product> data = new ArrayList<Product>();
		//	List<Product> reportIdAndName = new ArrayList<Product>();

			List<ActivityTools> list = getActivityToolService()
					.findByActivityid(activityId);
			request.setAttribute("eid", list.get(0).getEnterprise().getId());
			Long enterpriseId = 0L;
			Long toolId = 0L;
			Long solutionId = 0L;
			List<TReportTemplateEnActToolOrSol> actToolOrSols = new ArrayList<TReportTemplateEnActToolOrSol>();
			TReportTemplateEnActToolOrSolService actToolOrSolService = (TReportTemplateEnActToolOrSolService) Maxtech
					.getInstance().getBean(TReportTemplateEnActToolOrSolService.ID_NAME);
			
			int i = 0;
			List<Long> sList = new ArrayList<Long>();
			for (ActivityTools at : list) {
				Product map = new Product();
				Product reportId = null;
				if (at.getSolutionsZd() == null) {
					/*enterpriseId = at.getEnterprise().getId();
					toolId = at.getTools().getId();
					logger.info("enid--->>"+at.getEnterprise().getId());
					logger.info("toolId--->>"+at.getTools().getId());
					actToolOrSols = actToolOrSolService.findByEnterpriceAndToolsExport(enterpriseId, toolId);
					logger.info("t_size--->>"+actToolOrSols.size());
					if(!actToolOrSols.isEmpty()&&actToolOrSols!=null){
					for(TReportTemplateEnActToolOrSol et:actToolOrSols){
						reportId = new Product();
						reportId.settOrSId(et.getReportTemplate().getId());
						reportId.setName(et.getReportTemplate().getName());
						reportIdAndName.add(reportId);
					}}*/
					map.setId("t_" + at.getTools().getId());
					map.setName(at.getTools().getForeignName());
					data.add(map);
					i++;
				} else {
					if (sList.contains(at.getSolutionsZd().getId())) {
						continue;
					} else {
						/*enterpriseId = at.getEnterprise().getId();
						solutionId = at.getSolutionsZd().getId();
						logger.info("solutionId--->>"+at.getSolutionsZd().getId());
						actToolOrSols = actToolOrSolService.findByEnterpriceAndSolutionExport(enterpriseId, solutionId);
						logger.info("s_size--->>"+actToolOrSols.size());
						if(!actToolOrSols.isEmpty()&&actToolOrSols!=null){
						for(TReportTemplateEnActToolOrSol et:actToolOrSols){
							reportId = new Product();
							reportId.settOrSId(et.getReportTemplate().getId());
							reportId.setName(et.getReportTemplate().getName());
							reportIdAndName.add(reportId);
						}}*/
						map.setId("s_" + at.getSolutionsZd().getId());
						map.setName(at.getSolutionsZd().getForeignName());
						sList.add(at.getSolutionsZd().getId());
						data.add(map);
						i++;
					}
				}
			}
			request.setAttribute("produts", data);
		//	logger.info("reportIdAndName-->"+reportIdAndName.toString());
		//	request.setAttribute("reportIdAndName", reportIdAndName);
			return mapping.findForward(SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			printErrorJson(response, "操作失败,服务器出错!");
			return null;
		}

	}

	/*
	 * public ActionForward exportResult_old(ActionMapping mapping, ActionForm
	 * form, HttpServletRequest request, HttpServletResponse response) throws
	 * Exception { try { double begintime = System.currentTimeMillis();
	 * Enterprise sessionEnterprise = getSessionEnterprise(request); String
	 * product = ParamUtil.getStr(request, "selectPro", ""); Long activityId =
	 * ParamUtil.getLongParamter(request, "activityId", 0L); String beginDate =
	 * ParamUtil.getStr(request, "bDate", ""); String endDate =
	 * ParamUtil.getStr(request, "eDate", ""); if(sessionEnterprise == null){
	 * return null; } if("".equals(product) || activityId == 0 ){
	 * printErrorJson(response, "请选择测评产品！"); return null; } // boolean flag =
	 * ReportJobUtil.canGetFrom51(product); // if(!flag){ //
	 * printErrorJson(response, "无可用数据！"); // return null; // }
	 * List<ActivityPersons> apList =
	 * getActivityPersonService().findApsByActivityId(activityId);
	 * 
	 * List<ActivityPersons> list = getPersons(activityId, product, beginDate,
	 * endDate,apList); if(list == null || list.size() == 0){
	 * printErrorJson(response, "无可用数据！"); return null; }
	 * 
	 * ExportUtil util = new ExportUtil(); String file_path =
	 * util.export(product, list, sessionEnterprise.getId(), tool);
	 * //1、判断失败的各种情况 2、导出时提示“正在导出，请稍后!” if(file_path == null){
	 * printErrorJson(response, "导出失败！"); return null; }else
	 * if("Error1".equals(file_path)){ printErrorJson(response, "此产品暂时不支持导出！");
	 * return null; }else if("Error2".equals(file_path)){
	 * printErrorJson(response, "导出模板出错，导出失败！"); return null; }
	 * 
	 * // Activity activity = getActivityService().getById(activityId);
	 * StringBuffer proName = new StringBuffer(""); String[] pro =
	 * product.split("_"); if("t".equals(pro[0])){ ToolsService tService =
	 * (ToolsService)Maxtech.getInstance().getBean(ToolsService.ID_NAME); Tools
	 * tools = tService.getById(Long.parseLong(pro[1]));
	 * proName.append(tools.getForeignName()); }else{ SolutionsZdService
	 * sService =
	 * (SolutionsZdService)Maxtech.getInstance().getBean(SolutionsZdService
	 * .ID_NAME); SolutionsZd solutions =
	 * sService.getById(Long.parseLong(pro[1]));
	 * proName.append(solutions.getForeignName()); } SimpleDateFormat fmt = new
	 * SimpleDateFormat("yyyyMMdd"); String fileName =
	 * proName.toString()+"_"+fmt.format(new Date());
	 * 
	 * File filein = new File(file_path); FileInputStream excin = new
	 * FileInputStream(filein); BufferedInputStream bin = new
	 * BufferedInputStream(excin);
	 * response.setContentType("application/"+"ms-excel");
	 * response.setHeader("Content-Disposition", "attachment;filename=\"" + new
	 * String(fileName.getBytes("GB2312"), "iso-8859-1") + ".xls\"");
	 * OutputStream fos = response.getOutputStream();
	 * 
	 * byte[] b = new byte[5000]; int i = 0; while ((i = bin.read(b)) != -1) {
	 * fos.write(b, 0, i); } fos.flush(); bin.close(); fos.close();
	 * 
	 * excin.close(); filein.delete();
	 * 
	 * double endtime = System.currentTimeMillis();
	 * System.out.println("整个导出耗时："+(endtime-begintime)); return null;
	 * 
	 * 
	 * } catch (Exception e) { e.printStackTrace(); } return null; }
	 */
	// public ActionForward exportResultNew(ActionMapping mapping, ActionForm
	// form,
	// HttpServletRequest request, HttpServletResponse response)
	// throws Exception {
	//
	// List<Long> apIdList = new ArrayList<Long>();
	// double begintime = System.currentTimeMillis();
	// Enterprise sessionEnterprise = getSessionEnterprise(request);
	// String product = ParamUtil.getStr(request, "selectPro", "");
	// boolean isSolution = false;//是否是方案还是产品
	// Long p_id = null; //产品id
	// String proName = "";//产品对外名称
	// ExportUtil util = new ExportUtil();
	// HttpSession session = request.getSession();
	//
	// session.removeAttribute("TOTALCOUNT");
	// session.removeAttribute("CURRCOUNT");
	// session.removeAttribute("STARTTIME");
	//
	// if (sessionEnterprise == null) {
	// status_count = ExportUtil.EXPORTSCORE_FINISH;
	// System.out.println("status============================" + status_count);
	// return null;
	// }
	//
	// //产品信息
	// String[] pro = product.split("_");
	// Tools tools = null;
	// SolutionsZd solutions = null;
	// p_id = Long.parseLong(pro[1]);
	// if ("t".equals(pro[0])) {
	// ToolsService tService = (ToolsService) Maxtech.getInstance()
	// .getBean(ToolsService.ID_NAME);
	// tools = tService.getById(p_id);
	// proName = tools.getForeignName();
	// } else {
	// SolutionsZdService sService = (SolutionsZdService) Maxtech.getInstance()
	// .getBean(SolutionsZdService.ID_NAME);
	// solutions = sService.getById(Long.parseLong(pro[1]));
	// proName = solutions.getForeignName();
	// isSolution = true;
	// }
	//
	// //获取产品类型，如果是专业题，特殊处理,所有专业题默认使用统一模板
	// boolean isZYTool = false;
	// if (tools != null && tools.getToolsType() == 8)
	// isZYTool = true;
	// String fileName = product + "_" + sessionEnterprise.getId() + ".xls";
	// if (isZYTool)
	// fileName = "t_zy_" + sessionEnterprise.getId() + ".xls";
	// String path = util.getExcelTemplateFile(fileName);
	// File file = new File(path);
	// if (!file.exists()) {
	// System.out.println("没有企业定制的导出模板，将使用产品的默认模板导出！");
	// fileName = product + ".xls";
	// if (isZYTool)
	// fileName = "t_zy.xls";
	// path = util.getExcelTemplateFile(fileName);
	//
	// file = new File(path);
	// if (!file.exists()) {
	// System.out.println("未找到模板文件！------" + product + ".xls");
	// printErrorJson(response, "导出成绩时未找到模板文件！！！");
	// status_count = ExportUtil.EXPORTSCORE_FINISH;
	// return null;
	// }
	// }
	//
	// //整理apid和主库、分库的关系
	// HashMap<Integer, List> dbIdapListMap = new HashMap<Integer, List>();
	// for( Long apid : apIdList )
	// {
	// ActivityPersons ap = getActivityPersonService().getById(apid);
	// String dbid = String.valueOf(new
	// CustomerContextHolder().getDbIdByseqCode(ap.getSeqCode()));
	//
	// List<Long> dbApIdList = dbIdapListMap.get(new Integer(dbid));
	// if( dbApIdList == null )
	// dbApIdList = new ArrayList<Long>();
	//
	// dbApIdList.add(ap.getId());
	// dbIdapListMap.put(new Integer(dbid), dbApIdList);
	// }
	//
	// //根据分库数量进行查找
	// int dbNum = ExportUtil.getDBNum();
	// if( dbNum == -1 )
	// {
	// status_count = ExportUtil.EXPORTSCORE_FINISH;
	// System.out.println("status============================" + status_count);
	// return null;
	// }
	// status_count = ExportUtil.EXPORTSCORE_GETPEOPLES;//开始导出，记录状态
	// System.out.println("status============================" + status_count);
	//
	// CustomerContextHolder cch = new CustomerContextHolder();
	//
	// cch.setDynamicDataSource("0");
	// System.out.println("获取全部作答人数，耗时：" + (System.currentTimeMillis() -
	// begintime));
	// session.setAttribute("TOTALCOUNT", apIdList.size());
	// if (apIdList.size() == 0) {
	// System.out.println("企业HR导出分数人员==0");
	// // printErrorJson(response,"没有符合查询条件的人员，请重新选择查询条件");
	// response.setCharacterEncoding("utf-8");
	// response.getWriter().println(
	// //
	// "<script>alert('没有符合查询条件的人员，请重新选择查询条件！');window.parent.addTab(\"成绩导出\",\"toExportScore.do?method=getProducts&activityId="+activityId+"\");</script>");
	// "<script>alert('没有符合查询条件的人员，请重新选择查询条件！');window.location.href='toExportScore.do?method=getProducts&activityId="
	// + activityId + "';</script>");
	// status_count = ExportUtil.EXPORTSCORE_FINISH;
	// return null;
	// }
	//
	// status_count = ExportUtil.EXPORTSCORE_CHARGE;
	// System.out.println("status============================" + status_count);
	//
	// //检查扣费是否成功
	// if( !isChargingOk( apIdList, product ) )
	// {
	// status_count = ExportUtil.EXPORTSCORE_FINISH;
	// return null;
	// }
	// System.out.println("扣费，耗时：" + (System.currentTimeMillis() - begintime));
	//
	//
	//
	// status_count = ExportUtil.EXPORTSCORE_EXPORT;
	// System.out.println("status============================" + status_count);
	// //开始准备导出数据
	// String template_path = this.getClass().getResource("/").getPath();
	// try {
	// String tmp_excel = "";
	// Workbook rwb = Workbook.getWorkbook(file);
	// Date now = new Date();
	//
	// tmp_excel = System.getProperty("java.io.tmpdir") + File.separator +
	// now.getTime()
	// + fileName;
	// System.out.println("HR导出临时文件：" + tmp_excel);
	// //打开excel
	// File tempfile = new File(tmp_excel);
	// // File tempfile = new File(url+"tempfile.xls");
	// WritableWorkbook wwb = Workbook.createWorkbook(tempfile, rwb);
	// WritableSheet ws = wwb.getSheet(0);
	//
	// WritableFont wFont = new WritableFont(WritableFont.ARIAL, 11);// table样式
	// WritableCellFormat wCellFormat = new WritableCellFormat(wFont);//
	// tableCell样式
	// wCellFormat.setAlignment(jxl.format.Alignment.CENTRE);// 水平方向居中
	// wCellFormat.setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);//
	// 竖直方向居中
	// wCellFormat.setBorder(jxl.format.Border.ALL,
	// jxl.format.BorderLineStyle.THIN);// 边框设置
	// wCellFormat.setWrap(true);// 自动换行
	//
	// //获取导出模板中的公式
	// // List<String> func_list = new ArrayList();
	// HashMap<Integer, String> func_list = new HashMap<Integer, String>();
	// HashMap<Integer, String> formula_list = new HashMap<Integer, String>();
	// int beginRow = util.getFunList(ws, func_list, formula_list);
	// if (beginRow == 0) {
	// printErrorJson(response, "获取导出模板公式出错，导出失败！");
	// status_count = ExportUtil.EXPORTSCORE_FINISH;
	// System.out.println("status============================" + status_count);
	// return null;
	// }
	// beginRow--;//回退到公式那行，覆盖公式
	// int sum_col = func_list.size() + formula_list.size();//excel中有效列数
	//
	// String file_path = "";
	// int cur_count = 0;
	// session.setAttribute("STARTTIME",
	// Calendar.getInstance().getTimeInMillis());
	//
	// for (int i = 1; i <= dbNum; i++) {
	// try {
	// List<Long> aptlist = new ArrayList<Long>();
	// cch.setDynamicDataSource(new Integer(i).toString());
	// aptlist = dbIdapListMap.get(new Integer(i));
	//
	// for (int j = 0; j < aptlist.size(); j++) { //因为已经用第一个人将函数公式一行替换了，所以下标从1开始
	// Long apid = aptlist.get(j);
	// cur_count++;
	// beginRow++;
	// double beginTime = System.currentTimeMillis();
	// util.writeOnePersonExcelScore(i, beginRow, sum_col, func_list,
	// formula_list, ws, aptlist.get(j), product, tools, wCellFormat);
	//
	//
	// double endTime = System.currentTimeMillis();
	// System.out.println("数据库:" + i + "企业HR分数导出：" + j + "/" + aptlist.size()
	// + "填写一个人" + aptlist.get(j) + "的信息耗时：" + (endTime - beginTime));
	// session.setAttribute("CURRCOUNT", cur_count);
	// System.out
	// .println("status============================current:" + cur_count);
	//
	// }
	//
	// aptlist.clear();
	// } catch (Exception ex) {
	// System.out.println("企业HR导出分数异常，" + ex.toString());
	// printErrorJson(response, "获取导出模板公式出错，导出失败！");
	// status_count = ExportUtil.EXPORTSCORE_FINISH;
	// return null;
	// } finally {
	// cch.setDynamicDataSource(new Integer(i).toString());
	// }
	//
	// }
	// wwb.write();
	// wwb.close();
	// rwb.close();
	// cch.setDynamicDataSource("0");
	//
	// SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
	// String output_fileName = proName.toString() + "_" + fmt.format(new
	// Date());
	//
	// File filein = new File(tempfile.getPath());
	// FileInputStream excin = new FileInputStream(filein);
	// BufferedInputStream bin = new BufferedInputStream(excin);
	// response.setContentType("application/" + "ms-excel");
	// response.setHeader("Content-Disposition", "attachment;filename=\""
	// + new String(output_fileName.getBytes("GB2312"), "iso-8859-1") +
	// ".xls\"");
	// OutputStream fos = response.getOutputStream();
	//
	// byte[] b = new byte[5000];
	// int i = 0;
	// while ((i = bin.read(b)) != -1) {
	// fos.write(b, 0, i);
	// }
	// fos.flush();
	// bin.close();
	// fos.close();
	//
	// excin.close();
	// filein.delete();
	//
	// double endtime = System.currentTimeMillis();
	// System.out.println("整个导出耗时：" + (endtime - begintime));
	// status_count = ExportUtil.EXPORTSCORE_FINISH;
	// return null;
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// cch.setDynamicDataSource("0");
	// }
	// status_count = ExportUtil.EXPORTSCORE_FINISH;
	// return null;
	// }
	
	
/**
 * 分数导出优化代码
 * @param mapping
 * @param form
 * @param request
 * @param response
 * @return
 * @throws Exception
 * @author YangXR
 */
	public ActionForward exportResult(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		double begintime = System.currentTimeMillis();
		Enterprise sessionEnterprise = getSessionEnterprise(request);
		String product = ParamUtil.getStr(request, "selectPro", "");
		Long activityId = ParamUtil.getLongParamter(request, "activityId", 0L);
		Long reportId = ParamUtil.getLongParamter(request, "tOrSId", 0L);
		logger.info("activityId--->" + activityId);// 3514
		logger.info("tOrSId--->"+reportId);
		String beginDate = ParamUtil.getStr(request, "bDate", "");
		String endDate = ParamUtil.getStr(request, "eDate", "");
		boolean isSolution = false;// 是否是方案还是产品
		Long p_id = null; // 产品id
		String proName = "";// 产品对外名称
		ExportUtil util = new ExportUtil();
		HttpSession session = request.getSession();

		session.removeAttribute("TOTALCOUNT");
		session.removeAttribute("CURRCOUNT");
		session.removeAttribute("STARTTIME");

		if (sessionEnterprise == null) {
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			System.out.println("status============================="
					+ status_count);
			return null;
		}
		if ("".equals(product) || activityId == 0) {
			printErrorJson(response, "请选择测评产品！");
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			System.out.println("status============================"
					+ status_count);
			return null;
		}
		
		Tools tools = null;
		SolutionsZd solutions = null;
		// 产品信息
		String[] pro = product.split("_");
		p_id = Long.parseLong(pro[1]);
		// 获取导出文件的 命名
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
			Map map = util.getFile(product,sessionEnterprise.getId(),reportId);
			Long enId = sessionEnterprise.getId();
			String fileName = "";
			File file = null;
			
			if(map!=null){
			if ("1".equals(map.get("errCode"))) {
				printErrorJson(response, "导出成绩时未找到模板文件！！！");
				return null;
			}
			if("0".equals(map.get("success"))){
			fileName = (String) map.get("fileName");
			file = (File) map.get("file");}
			}
		// 根据分库数量进行查找
		int dbNum = ExportUtil.getDBNum();
		if (dbNum == -1) {
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			System.out.println("status============================"
					+ status_count);
			return null;
		}

		status_count = ExportUtil.EXPORTSCORE_GETPEOPLES;// 开始导出，记录状态
		System.out.println("status============================" + status_count);
		// 收集导出成绩的人员信息
		HashMap<Integer, List> dbIdapListMap = new HashMap<Integer, List>();
		ActivityPersonToolsService aptService = (ActivityPersonToolsService) Maxtech
				.getInstance().getBean(ActivityPersonToolsService.ID_NAME);
		CustomerContextHolder cch = new CustomerContextHolder();
		List<Long> aptForChargelist = new ArrayList<Long>();
		List<Long> duplicateApId = new ArrayList<Long>();// 记录重复apid，脏数据过滤用
		// HashMap<Long,Long> hApIdDbId = new HashMap<Long,Long>();
		for (int i = 1; i <= dbNum; i++) {
			List<Long> aptlist = null;
			cch.setDynamicDataSource(new Integer(i).toString());
			if (isSolution) {
				SolutionsToolsService stService = (SolutionsToolsService) Maxtech
						.getInstance().getBean(SolutionsToolsService.ID_NAME);
				List<SolutionsTools> stList = stService.getBySolutionId(p_id);
				aptlist = aptService.getActivityPersonIDForSolution(activityId,
						p_id, 2, beginDate, endDate, stList.size());
			} else
				aptlist = aptService.getActivityPersonIDForTool(activityId,
						p_id, 2, beginDate, endDate);

			// 过滤脏数据
			for (Long apid : aptlist) {
				if (aptForChargelist.contains(apid))// 已经存在了,记录到重复列表中，从正常列表中删除，扣费列表作为最全的apid的列表
					duplicateApId.add(apid);
				else
					aptForChargelist.add(apid);
			}
			dbIdapListMap.put(new Integer(i), aptlist);// 只是没有重复的apid
			// aptForChargelist.addAll(aptlist);
			// aptForChargelist.r
		}

		cch.setDynamicDataSource("0");
		System.out.println("获取全部作答人数，耗时："
				+ (System.currentTimeMillis() - begintime));
		session.setAttribute("TOTALCOUNT", aptForChargelist.size());
		if (aptForChargelist.size() == 0) {
			System.out.println("企业HR导出分数人员==0");
			// printErrorJson(response,"没有符合查询条件的人员，请重新选择查询条件");
			response.setCharacterEncoding("utf-8");
			response.getWriter()
					.println(
					// "<script>alert('没有符合查询条件的人员，请重新选择查询条件！');window.parent.addTab(\"成绩导出\",\"toExportScore.do?method=getProducts&activityId="+activityId+"\");</script>");
							"<script>alert('没有符合查询条件的人员，请重新选择查询条件！');window.location.href='toExportScore.do?method=getProducts&activityId="
									+ activityId + "';</script>");
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			return null;
		}

		// 检查扣费是否成功
		if (!isChargingOk(aptForChargelist, product)) {
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			response.setCharacterEncoding("utf-8");
			response.getWriter()
					.println(
					// "<script>alert('导出成绩扣费失败，请检查剩余账号情况！');history.go(-1);</script>");
							"<script>alert('导出成绩扣费失败，请检查剩余账号情况！');window.location.href='toExportScore.do?method=getProducts&activityId="
									+ activityId + "';</script>");
			return null;
		}
		System.out.println("扣费，耗时：" + (System.currentTimeMillis() - begintime));
		status_count = ExportUtil.EXPORTSCORE_EXPORT;
		System.out.println("status============================" + status_count);
		
		
		// 开始准备导出数据
		// String template_path = this.getClass().getResource("/").getPath();
		MemcachedUtil memcachedUtil = MemcachedUtil.getInstance();
		//memcachedUtil.delete(fileName);
		MemcacheExport export = (MemcacheExport) memcachedUtil.get(fileName);
		
		String jsonArrays = util.exportComment(product,null, null,null,
				 dbIdapListMap, 
				 enId, reportId,
				 duplicateApId,request );
		
		
		//JSONObject json = new JSONObject(JsonInfo); 
		if("Error1".equals(jsonArrays)){
			logger.info("文件不存在");
			printErrorJson(response, "导出成绩时未找到模板文件！！！");
			return null;
		}
		if("Error2".equals(jsonArrays)){
			printErrorJson(response, "获取导出模板公式出错，导出失败！");
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			System.out.println("status============================"
					+ status_count);
			return null;
		}
		String[] strError=jsonArrays.split("_");
		if("-1".equals(strError[0])){
			System.out.println("企业HR导出分数异常，" +strError[1]);
			printErrorJson(response, "获取导出模板公式出错，导出失败！");
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			System.out.println("status============================"
					+ status_count);
			return null;
		}
		
			/*	//解析json
				MemcacheExport export3 = (MemcacheExport)memcachedUtil.get(fileName);
				JSONArray jsonArrs = JSONArray.fromObject(jsonArrays);
		        String aS[] = new String[export3.getListHeader().size()];
		        String header1="";
		        for(int i=0;i<jsonArrs.size();i++){//i 代表几个人； j代表 表头
		        	for(int j=0;j<export3.getListHeader().size();j++){
						header1 = (String) export3.getListHeader().get(j);
						aS[j] = jsonArrs.getJSONObject(i).getString(header1);
						System.out.println(aS[j]);
					}
		        }*/
		
		
		try {
			String tmp_excel = "";
			Workbook rwb = Workbook.getWorkbook(file);
			Date now = new Date();
			tmp_excel = System.getProperty("java.io.tmpdir") + File.separator
					+ now.getTime() + fileName;
			System.out.println("HR导出临时文件：" + tmp_excel);
			System.out.println("HR导出临时文件hhh：" + File.separator);
			// 打开excel
			File tempfile = new File(tmp_excel);
			WritableWorkbook wwb = Workbook.createWorkbook(tempfile, rwb);
			WritableSheet ws = wwb.getSheet(0);
			Label label = null;
			
			WritableFont wFont = new WritableFont(WritableFont.ARIAL, 11);// table样式
			WritableCellFormat wCellFormat = new WritableCellFormat(wFont);// tableCell样式
			wCellFormat.setAlignment(jxl.format.Alignment.CENTRE);// 水平方向居中
			wCellFormat.setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);// 竖直方向居中
			wCellFormat.setBorder(jxl.format.Border.ALL,jxl.format.BorderLineStyle.THIN);// 边框设置
			wCellFormat.setWrap(true);// 自动换行
			
			//解析json
			MemcacheExport export3 = (MemcacheExport)memcachedUtil.get(fileName);
			JSONArray jsonArrsContent = JSONArray.fromObject(jsonArrays);
			System.out.println("我要测试这个jsonArrsContent--》"+jsonArrsContent.toString());
			JSONObject ob  = (JSONObject) jsonArrsContent.get(0);
			System.out.println("我要测试这个--》resultList--->"+ob.getString("resultList"));
			JSONArray jsonArrs =(JSONArray) JSONArray.fromObject(ob.getString("resultList"));//获取成绩部分的json
			System.out.println("我要测试这个获取成绩部分的json--》jsonArrs--->"+jsonArrs.size());
			System.out.println("我要测试这个获取成绩部分的json--》jsonArrs.toString()--->"+jsonArrs.toString());
	        String aS[] = new String[export3.getListHeader().size()];
	        String header="";
	        int cur_count = 0;
	    	session.setAttribute("STARTTIME", Calendar.getInstance().getTimeInMillis());
	        for(int i=0;i<jsonArrs.size();i++){//i 代表几个人； j代表 表头
	        	for(int j=0;j<export3.getListHeader().size();j++){
					header = (String) export3.getListHeader().get(j);
					aS[j] = jsonArrs.getJSONObject(i).getString(header);
					label = new Label(j,3+i,aS[j],wCellFormat);
					ws.addCell(label);
					System.out.println(aS[j]);
				}
	        	cur_count++;
	        	session.setAttribute("CURRCOUNT", cur_count);
	        }
			

						/*	//生成json
							MemcacheExport export2 = (MemcacheExport)memcachedUtil.get(fileName);
							String sorce = "";
							String header = "";
							int arrLength = export2.getListHeader().size();
							 //String[][] excelContent = new String[arrLength][arrLength]; 
							JSONArray jsonArray = new JSONArray();
							List lists = new ArrayList();
							for(int i=0;i<lists.size();i++){  //循环成绩次数 i 代表 几个人  j 代表 表头循环
								System.out.println("\t"+"***************第"+i+"个人的成绩**********");
								Map map = new HashMap();
								LinkedHashMap hMap = new LinkedHashMap();
								for(int j=0;j<export2.getListHeader().size();j++){
									header = (String) export2.getListHeader().get(j);
									List list  = (List) lists.get(i);
									sorce = (String) list.get(j);
									//excelContent[j][j]=excelContent[header][sorce];
									System.out.println(header+":"+sorce);
									hMap.put(header, sorce);
								}
								jsonArray.add(hMap);
							}
							System.out.println(jsonArray.toString());
							//解析json
							JSONArray jsonArr = JSONArray.fromObject(jsonArray.toString());
				            String a[] = new String[export2.getListHeader().size()];
				            for(int i=0;i<jsonArr.size();i++){//i 代表几个人； j代表 表头
				            	for(int j=0;j<export2.getListHeader().size();j++){
									header = (String) export2.getListHeader().get(j);
									a[j] = jsonArr.getJSONObject(i).getString(header);
									System.out.println(a[j]);
								}
				            }*/
            
			wwb.write();
			wwb.close();
			rwb.close();
			cch.setDynamicDataSource("0");

			SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
			String output_fileName = proName.toString() + "_"
					+ fmt.format(new Date());
			System.out.println("output_fileName---------->" + output_fileName);
			logger.info("output_fileName---------->" + output_fileName);

			File filein = new File(tempfile.getPath());
			FileInputStream excin = new FileInputStream(filein);
			BufferedInputStream bin = new BufferedInputStream(excin);
			response.setContentType("application/" + "ms-excel");
			response.setHeader("Content-Disposition", "attachment;filename=\""
					+ new String(output_fileName.getBytes("GB2312"),
							"iso-8859-1") + ".xls\"");
			OutputStream fos = response.getOutputStream();

			byte[] b = new byte[5000];
			int i = 0;
			while ((i = bin.read(b)) != -1) {
				fos.write(b, 0, i);
			}
			fos.flush();
			bin.close();
			fos.close();

			excin.close();
			filein.delete();

			double endtime = System.currentTimeMillis();
			System.out.println("整个导出耗时：" + (endtime - begintime));
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			return null;

		} catch (Exception e) {
			e.printStackTrace();
			cch.setDynamicDataSource("0");
		}
		status_count = ExportUtil.EXPORTSCORE_FINISH;
		return null;
	}
	
	
/**
 * 原始分数导出代码
 * @param mapping
 * @param form
 * @param request
 * @param response
 * @return
 * @throws Exception
 */
	public ActionForward exportResultAA(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		double begintime = System.currentTimeMillis();
		Enterprise sessionEnterprise = getSessionEnterprise(request);
		String product = ParamUtil.getStr(request, "selectPro", "");
		Long activityId = ParamUtil.getLongParamter(request, "activityId", 0L);
		Long reportId = ParamUtil.getLongParamter(request, "tOrSId", 0L);
		logger.info("activityId--->" + activityId);// 3514
		logger.info("tOrSId--->"+reportId);
		String beginDate = ParamUtil.getStr(request, "bDate", "");
		String endDate = ParamUtil.getStr(request, "eDate", "");
		boolean isSolution = false;// 是否是方案还是产品
		Long p_id = null; // 产品id
		String proName = "";// 产品对外名称
		ExportUtil util = new ExportUtil();
		HttpSession session = request.getSession();

		session.removeAttribute("TOTALCOUNT");
		session.removeAttribute("CURRCOUNT");
		session.removeAttribute("STARTTIME");

		if (sessionEnterprise == null) {
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			System.out.println("status============================="
					+ status_count);
			return null;
		}
		if ("".equals(product) || activityId == 0) {
			printErrorJson(response, "请选择测评产品！");
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			System.out.println("status============================"
					+ status_count);
			return null;
		}

		Tools tools = null;
		SolutionsZd solutions = null;
		// 产品信息
		String[] pro = product.split("_");
		/*
		 * Tools tools = null; SolutionsZd solutions = null;杨旭日注释
		 */
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
		Long enId = sessionEnterprise.getId();
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
				printErrorJson(response, "导出成绩时未找到模板文件！！！");
				return null;
			}
		}
		if ( "".equals(excelPath) || null == excelPath ) {

			// 获取产品类型，如果是专业题，特殊处理,所有专业题默认使用统一模板
			boolean isZYTool = false;
			if (tools != null && (tools.getToolsType() == 8 ||19 == tools.getToolsType()))
				isZYTool = true;
			// String fileName = product + "_" + sessionEnterprise.getId() +
			// ".xls"; 杨旭日注释
			fileName = product + "_" + sessionEnterprise.getId() + ".xls";
			logger.info("fileName--->" + fileName);
			if (isZYTool) {
				fileName = "t_zy_" + sessionEnterprise.getId() + ".xls";
				logger.info("fileName--->" + fileName);
			}
			/*
			 * String path = util.getExcelTemplateFile(fileName); File file =
			 * new File(path); 杨旭日注释
			 */
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
					System.out.println("未找到模板文件！------" + product + ".xls");
					printErrorJson(response, "导出成绩时未找到模板文件！！！");
					status_count = ExportUtil.EXPORTSCORE_FINISH;
					return null;
				}
			}
		}
		// 根据分库数量进行查找
		int dbNum = ExportUtil.getDBNum();
		if (dbNum == -1) {
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			System.out.println("status============================"
					+ status_count);
			return null;
		}

	//	status_count = ExportUtil.EXPORTSCORE_GETPEOPLES;// 开始导出，记录状态
	//	System.out.println("status============================" + status_count);
		// 收集导出成绩的人员信息
		HashMap<Integer, List> dbIdapListMap = new HashMap<Integer, List>();
		ActivityPersonToolsService aptService = (ActivityPersonToolsService) Maxtech
				.getInstance().getBean(ActivityPersonToolsService.ID_NAME);
		CustomerContextHolder cch = new CustomerContextHolder();
		List<Long> aptForChargelist = new ArrayList<Long>();
		List<Long> duplicateApId = new ArrayList<Long>();// 记录重复apid，脏数据过滤用
		// HashMap<Long,Long> hApIdDbId = new HashMap<Long,Long>();
		for (int i = 1; i <= dbNum; i++) {
			List<Long> aptlist = null;
			cch.setDynamicDataSource(new Integer(i).toString());
			if (isSolution) {
				SolutionsToolsService stService = (SolutionsToolsService) Maxtech
						.getInstance().getBean(SolutionsToolsService.ID_NAME);
				List<SolutionsTools> stList = stService.getBySolutionId(p_id);
				aptlist = aptService.getActivityPersonIDForSolution(activityId,
						p_id, 2, beginDate, endDate, stList.size());
			} else
				aptlist = aptService.getActivityPersonIDForTool(activityId,
						p_id, 2, beginDate, endDate);

			// 过滤脏数据
			for (Long apid : aptlist) {
				if (aptForChargelist.contains(apid))// 已经存在了,记录到重复列表中，从正常列表中删除，扣费列表作为最全的apid的列表
					duplicateApId.add(apid);
				else
					aptForChargelist.add(apid);
			}
			dbIdapListMap.put(new Integer(i), aptlist);// 只是没有重复的apid
			// aptForChargelist.addAll(aptlist);
			// aptForChargelist.r
		}

		cch.setDynamicDataSource("0");
		System.out.println("获取全部作答人数，耗时："
				+ (System.currentTimeMillis() - begintime));
		session.setAttribute("TOTALCOUNT", aptForChargelist.size());
		if (aptForChargelist.size() == 0) {
			System.out.println("企业HR导出分数人员==0");
			// printErrorJson(response,"没有符合查询条件的人员，请重新选择查询条件");
			response.setCharacterEncoding("utf-8");
			response.getWriter()
					.println(
					// "<script>alert('没有符合查询条件的人员，请重新选择查询条件！');window.parent.addTab(\"成绩导出\",\"toExportScore.do?method=getProducts&activityId="+activityId+"\");</script>");
							"<script>alert('没有符合查询条件的人员，请重新选择查询条件！');window.location.href='toExportScore.do?method=getProducts&activityId="
									+ activityId + "';</script>");
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			return null;
		}

		// 检查扣费是否成功
		if (!isChargingOk(aptForChargelist, product)) {
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			response.setCharacterEncoding("utf-8");
			response.getWriter()
					.println(
					// "<script>alert('导出成绩扣费失败，请检查剩余账号情况！');history.go(-1);</script>");
							"<script>alert('导出成绩扣费失败，请检查剩余账号情况！');window.location.href='toExportScore.do?method=getProducts&activityId="
									+ activityId + "';</script>");
			return null;
		}

		// if (aptForChargelist.size() > 0) {
		// status_count = ExportUtil.EXPORTSCORE_CHARGE;
		// System.out.println("status============================" +
		// status_count);
		// //检查扣费
		// // ActivityToolService ats = getActivityToolService();
		// // List<ActivityTools> listTools = ats.findByActivityid(activityId);
		//
		// Map<String, String> proApIdsHasmap = new HashMap<String, String>();
		// StringBuffer apIDs = new StringBuffer();
		// for (int i = 0; i < aptForChargelist.size(); i++) {
		// apIDs.append(aptForChargelist.get(i));
		// if (i != (aptForChargelist.size() - 1)) {
		// apIDs.append(",");
		// }
		// }
		// proApIdsHasmap.put(product, apIDs.toString());
		// ProductsPersonsService ppService = (ProductsPersonsService)
		// Maxtech.getInstance()
		// .getBean(ProductsPersonsService.ID_NAME);
		// Map<String, String> downloadProduct =
		// ppService.ischargeUpdate(proApIdsHasmap);
		// if ("false".equals(downloadProduct.get(product))) {
		// System.out.println("导出成绩扣费失败");
		// // printErrorJson(response,"导出成绩扣费失败，请检查剩余账号情况！");
		// response.setCharacterEncoding("utf-8");
		// response.getWriter().println(
		// // "<script>alert('导出成绩扣费失败，请检查剩余账号情况！');history.go(-1);</script>");
		// "<script>alert('导出成绩扣费失败，请检查剩余账号情况！');window.location.href='toExportScore.do?method=getProducts&activityId="
		// + activityId + "';</script>");
		// status_count = ExportUtil.EXPORTSCORE_FINISH;
		// return null;
		// }
		// }

		System.out.println("扣费，耗时：" + (System.currentTimeMillis() - begintime));
		status_count = ExportUtil.EXPORTSCORE_EXPORT;
		System.out.println("status============================" + status_count);
		// 开始准备导出数据
		// String template_path = this.getClass().getResource("/").getPath();
		MemcachedUtil memcachedUtil = MemcachedUtil.getInstance();
		memcachedUtil.delete(fileName);
		MemcacheExport export = (MemcacheExport) memcachedUtil.get(fileName);
		try {
			String tmp_excel = "";
			Workbook rwb = Workbook.getWorkbook(file);
			Date now = new Date();
			tmp_excel = System.getProperty("java.io.tmpdir") + File.separator
					+ now.getTime() + fileName;
			System.out.println("HR导出临时文件：" + tmp_excel);
			System.out.println("HR导出临时文件hhh：" + File.separator);
			// 打开excel
			File tempfile = new File(tmp_excel);
			// File tempfile = new File(url+"tempfile.xls");
			WritableWorkbook wwb = Workbook.createWorkbook(tempfile, rwb);
			WritableSheet ws = wwb.getSheet(0);

			WritableFont wFont = new WritableFont(WritableFont.ARIAL, 11);// table样式
			WritableCellFormat wCellFormat = new WritableCellFormat(wFont);// tableCell样式
			wCellFormat.setAlignment(jxl.format.Alignment.CENTRE);// 水平方向居中
			wCellFormat.setVerticalAlignment(jxl.format.VerticalAlignment.CENTRE);// 竖直方向居中
			wCellFormat.setBorder(jxl.format.Border.ALL,jxl.format.BorderLineStyle.THIN);// 边框设置
			wCellFormat.setWrap(true);// 自动换行

			// 获取导出模板中的公式
			// List<String> func_list = new ArrayList();
			HashMap<Integer, String> func_list = new HashMap();
			HashMap<Integer, String> formula_list = new HashMap();
			int beginRow = 0;
			if(export==null){
				beginRow = util.getMemache(fileName, ws, func_list, formula_list, rwb);
			}else{
				beginRow = export.getBeginRow();
				func_list = export.getFunc_list();
				formula_list = export.formula_list;
			}
			if (beginRow == 0) {
				printErrorJson(response, "获取导出模板公式出错，导出失败！");
				status_count = ExportUtil.EXPORTSCORE_FINISH;
				System.out.println("status============================"
						+ status_count);
				return null;
			}
			beginRow--;// 回退到公式那行，覆盖公式
			int sum_col = func_list.size() + formula_list.size();// excel中有效列数

			String file_path = "";
			int cur_count = 0;
			session.setAttribute("STARTTIME", Calendar.getInstance()
					.getTimeInMillis());
			List lists = new ArrayList();
			for (int i = 1; i <= dbNum; i++) {
				try {
					List<Long> aptlist = new ArrayList();
					cch.setDynamicDataSource(new Integer(i).toString());
					aptlist = dbIdapListMap.get(new Integer(i));

					for (int j = 0; j < aptlist.size(); j++) { // 因为已经用第一个人将函数公式一行替换了，所以下标从1开始
						Long apid = aptlist.get(j);
						// 处理重复数据
						if (duplicateApId.contains(apid)) {
							ActivityPersons ap = getActivityPersonService()
									.getById(apid);
							String dbid = String
									.valueOf(new CustomerContextHolder()
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
					
						util.writeOnePersonExcelScore(i, beginRow, sum_col,
								func_list, formula_list, ws, aptlist.get(j),
								product, tools, wCellFormat);
					 lists.add(util.getOnePersonExcelScore(i, beginRow, sum_col,
								func_list, formula_list,  aptlist.get(j),
								product, tools));
						

						/*
						 * 
						 * 
						 * 
						 * cch.setDynamicDataSource(new
						 * Integer(i+1).toString()); HashMap hApResultData = new
						 * HashMap(); HashMap hApInfoData = new HashMap();
						 * 
						 * //可能需要从分库取数据 beginRow++; for(int k=0; k<sum_col;
						 * k++){ if( func_list.get(k)!=null ) {
						 * if("".equals(func_list.get(k))){ ws.addCell(new
						 * Label(k, beginRow, "", wCellFormat)); continue; }
						 * Calendar fun_begin = Calendar.getInstance(); String
						 * cellValue = ""; try { cellValue =
						 * util.analyzeFunc(hApResultData
						 * ,hApInfoData,func_list.get(k), aptlist.get(j),
						 * product, tools, i+1); }catch( Exception ex ) {
						 * System.out.println( "aptid="+aptlist.get(j)+"公式："+
						 * func_list.get(k)+"成绩异常......"); ex.printStackTrace();
						 * cellValue = "N/A"; }
						 * System.out.println("公式"+func_list.get(k)+"耗时:"+
						 * (System
						 * .currentTimeMillis()-fun_begin.getTimeInMillis()));
						 * if( func_list.get(k).startsWith("%#MSCORE")&&
						 * cellValue !=null && !("".equals(cellValue))
						 * )//如果是分数，设置为数字格式 { jxl.write.Number lb2 = new
						 * jxl.write.Number(k, beginRow, new
						 * Integer(cellValue).intValue(),wCellFormat);
						 * ws.addCell(lb2); } else ws.addCell(new Label(k,
						 * beginRow, cellValue, wCellFormat)); } if(
						 * formula_list.get(k)!=null ) {
						 * 
						 * String formula = formula_list.get(k); formula =
						 * formula.replace("{$row$}", (beginRow+1)+"");
						 * ws.addCell(new
						 * Formula(k,beginRow,formula,wCellFormat));
						 * 
						 * } }
						 */
						double endTime = System.currentTimeMillis();
						System.out.println("数据库:" + i + "企业HR分数导出：" + j + "/"
								+ aptlist.size() + "填写一个人" + aptlist.get(j)
								+ "的信息耗时：" + (endTime - beginTime));
						session.setAttribute("CURRCOUNT", cur_count);
						System.out
								.println("status============================current:"
										+ cur_count);

					}

					aptlist.clear();
				} catch (Exception ex) {
					System.out.println("企业HR导出分数异常，" + ex.toString());
					printErrorJson(response, "获取导出模板公式出错，导出失败！");
					status_count = ExportUtil.EXPORTSCORE_FINISH;
					return null;
				} finally {
					cch.setDynamicDataSource(new Integer(i).toString());
				}

			}
			//生成json
			MemcacheExport export2 = (MemcacheExport)memcachedUtil.get(fileName);
			String sorce = "";
			String header = "";
			int arrLength = export2.getListHeader().size();
			 //String[][] excelContent = new String[arrLength][arrLength]; 
			JSONArray jsonArray = new JSONArray();
			for(int i=0;i<lists.size();i++){  //循环成绩次数 i 代表 几个人  j 代表 表头循环
				System.out.println("\t"+"***************第"+i+"个人的成绩**********");
				Map map = new HashMap();
				LinkedHashMap hMap = new LinkedHashMap();
				for(int j=0;j<export2.getListHeader().size();j++){
					header = (String) export2.getListHeader().get(j);
					List list  = (List) lists.get(i);
					sorce = (String) list.get(j);
					//excelContent[j][j]=excelContent[header][sorce];
					System.out.println(header+":"+sorce);
					hMap.put(header, sorce);
				}
				jsonArray.add(hMap);
			}
			System.out.println(jsonArray.toString());
			//解析json
			JSONArray jsonArr = JSONArray.fromObject(jsonArray.toString());
            String a[] = new String[export2.getListHeader().size()];
            for(int i=0;i<jsonArr.size();i++){//i 代表几个人； j代表 表头
            	for(int j=0;j<export2.getListHeader().size();j++){
					header = (String) export2.getListHeader().get(j);
					a[j] = jsonArr.getJSONObject(i).getString(header);
					System.out.println(a[j]);
				}
            }
            
			wwb.write();
			wwb.close();
			rwb.close();
			cch.setDynamicDataSource("0");

			SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
			String output_fileName = proName.toString() + "_"
					+ fmt.format(new Date());
			System.out.println("output_fileName---------->" + output_fileName);
			logger.info("output_fileName---------->" + output_fileName);

			File filein = new File(tempfile.getPath());
			FileInputStream excin = new FileInputStream(filein);
			BufferedInputStream bin = new BufferedInputStream(excin);
			response.setContentType("application/" + "ms-excel");
			response.setHeader("Content-Disposition", "attachment;filename=\""
					+ new String(output_fileName.getBytes("GB2312"),
							"iso-8859-1") + ".xls\"");
			OutputStream fos = response.getOutputStream();

			byte[] b = new byte[5000];
			int i = 0;
			while ((i = bin.read(b)) != -1) {
				fos.write(b, 0, i);
			}
			fos.flush();
			bin.close();
			fos.close();

			excin.close();
			filein.delete();

			double endtime = System.currentTimeMillis();
			System.out.println("整个导出耗时：" + (endtime - begintime));
			status_count = ExportUtil.EXPORTSCORE_FINISH;
			return null;

		} catch (Exception e) {
			e.printStackTrace();
			cch.setDynamicDataSource("0");
		}
		status_count = ExportUtil.EXPORTSCORE_FINISH;
		return null;
	}

	public ActionForward getExportResultStatus(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// 设置该响应不在缓存中读取
		response.addHeader("Expires", "0");
		response.addHeader("Cache-Control",
				"no-store, no-cache, must-revalidate");
		response.addHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "no-cache");
		response.setCharacterEncoding("utf-8");
		response.setContentType("text/html; charset=utf-8");

		int total_count = 0;// 总数据的条数

		int cur_count = 0;// 已导入数据条数

		// long getElapsedTimeInSeconds=0;// 获得已经上传得时间

		String message = "";
		HttpSession session = request.getSession();
		if (status_count == ExportUtil.EXPORTSCORE_INIT) {
			session.removeAttribute("TOTALCOUNT");
			session.removeAttribute("CURRCOUNT");
			session.removeAttribute("STARTTIME");
			message = "准备进行数据导出。。。。。。";
			this.printSuccessJson(response, message);
			return null;
		}

		if (status_count == ExportUtil.EXPORTSCORE_GETPEOPLES) {
			message = "获取测评人员信息。。。。。。";
			this.printSuccessJson(response, message);
			return null;
		}

		if (status_count == ExportUtil.EXPORTSCORE_CHARGE) {
			message = "正在进行扣费处理。。。。。。";
			this.printSuccessJson(response, message);
			return null;
		}
		if (status_count == ExportUtil.EXPORTSCORE_EXPORT) {
			if (session.getAttribute("TOTALCOUNT") != null) {
				total_count = Integer.parseInt(session.getAttribute(
						"TOTALCOUNT").toString());
			}
			if (session.getAttribute("CURRCOUNT") != null) {
				cur_count = Integer.parseInt(session.getAttribute("CURRCOUNT")
						.toString());
			}

			// System.out.println(session.getAttribute("STARTTIME") );
			// long stime = 0;
			// if (session.getAttribute("STARTTIME") != null) {
			//
			// stime=new Long(session.getAttribute("STARTTIME").toString());
			//
			// getElapsedTimeInSeconds=(System.currentTimeMillis() - stime) /
			// 1000;
			//
			// }

			// 计算上传完成的百分比

			String percentComplete = "0";
			BigDecimal big = new BigDecimal(1);
			if (total_count > 0) {

				double k = (double) cur_count / total_count * 100;

				big = new BigDecimal(k);

				percentComplete = big.setScale(2, BigDecimal.ROUND_HALF_UP)
						.toString();

			}

			// 获得上传已用的时间

			// long timeInSeconds = getElapsedTimeInSeconds;
			// long remainderSeconds = 0;
			// if( big.setScale(2,BigDecimal.ROUND_HALF_UP).longValue() > 0 )
			// remainderSeconds =
			// getElapsedTimeInSeconds/big.setScale(2,BigDecimal.ROUND_HALF_UP).longValue()*100;

			// 计算平均上传速率

			// double uploadRate = bytesRead / (timeInSeconds + 0.00001);

			// System.out.println("**************计算平均上传速率="+bytesRead);

			// 计算总共所需时间

			// double estimatedRuntime = totalSize / (uploadRate + 0.00001);

			// 将上传状态返回给客户端

			// message = "<b>导出状态:</b><br/>";
			message += "<br /><div class=\"prog-border\"><div class=\"prog-bar\" style=\"width: "

					+ percentComplete + "%;\"></div></div>";
			message += "导出: 第  " + cur_count + " 条/ 总共 " + total_count + " 条"

			+ " 进度 (" + percentComplete + "%)。";
			// if( cur_count == total_count )
			// message +=
			// "导出成功。耗时："+formatTime(Calendar.getInstance().getTimeInMillis()-stime)+"<br/>";
			// else
			// message +=
			// "用时："+formatTime(getElapsedTimeInSeconds)+"，预计还需要："+formatTime(remainderSeconds)+" <br/>";
			if (cur_count == total_count)
				message += "导出成功。<br/>";
			// else
			// message +=
			// "用时："+formatTime(getElapsedTimeInSeconds)+"，预计还需要："+formatTime(remainderSeconds)+" <br/>";

			printSuccessJson(response, message);

			return null;

		}
		if (status_count == ExportUtil.EXPORTSCORE_FINISH) {
			if (session.getAttribute("TOTALCOUNT") != null) {
				total_count = Integer.parseInt(session.getAttribute(
						"TOTALCOUNT").toString());
			}
			if (session.getAttribute("CURRCOUNT") != null) {
				cur_count = Integer.parseInt(session.getAttribute("CURRCOUNT")
						.toString());
			}
			if (total_count != 0) {
				message += "<br /><div class=\"prog-border\"><div class=\"prog-bar\" style=\"width: 100%;\"></div></div>";
				message += "导出: 第  " + cur_count + " 条/ 总共 " + total_count
						+ " 条"

						+ " 进度 (100.00%)。";
				// long stime = 0;
				// if (session.getAttribute("STARTTIME") != null) {
				// stime=new Long(session.getAttribute("STARTTIME").toString());
				// }
				// if( cur_count == total_count )
				// message +=
				// "导出成功。耗时："+formatTime(Calendar.getInstance().getTimeInMillis()-stime)+"<br/>";
				status_count = ExportUtil.EXPORTSCORE_INIT;
				if (cur_count == total_count)
					message += "导出成功。<br/>";

			}
			session.removeAttribute("TOTALCOUNT");
			session.removeAttribute("CURRCOUNT");
			session.removeAttribute("STARTTIME");
			status_count = ExportUtil.EXPORTSCORE_INIT;
			this.printErrorJson(response, message);
			return null;
		}
		return null;

	}

	private String formatTime(double timeInSeconds) {

		long seconds = (long) Math.floor(timeInSeconds);

		long minutes = (long) Math.floor(timeInSeconds / 60.0);

		long hours = (long) Math.floor(minutes / 60.0);

		if (hours != 0) {

			return hours + "小时 " + (minutes % 60) + "分钟 "

			+ (seconds % 60) + "秒";

		} else if (minutes % 60 != 0) {

			return (minutes % 60) + "分钟 " + (seconds % 60) + "秒";

		} else {

			return (seconds % 60) + " 秒";

		}

	}

	// 页面调用此方法，检查导入工作是否正在进行时，0：无 1：正在进行
	public int checkImportStatus() {

		return status_count;

	}

	// 筛选符合条件的人
	public List<ActivityPersons> getPersons(Long activityId, String product,
			String beginDate, String endDate, List<ActivityPersons> apList)
			throws Exception {
		try {
			ActivityPersonToolsService aptService = (ActivityPersonToolsService) Maxtech
					.getInstance().getBean(ActivityPersonToolsService.ID_NAME);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			List<ActivityPersons> list_result = new ArrayList<ActivityPersons>();
			String[] pro = product.split("_");

			System.out.println("export score:" + apList.size()
					+ " persons......");
			Date begin = new Date(Calendar.getInstance().getTimeInMillis());

			if ("t".equals(pro[0])) {
				// list_result =
				// getActivityPersonService().getByTestStatusAndTime(pro[1],
				// activityId, beginDate, endDate);

				// //循环查找每个分库
				// String pro_path =ReportBase.return_path();
				// PropertiesUtil proUtil = new PropertiesUtil(pro_path+
				// "/db.properties");
				// if(proUtil == null){
				// System.out.println("db.properties没有找到！！！");
				// return null;
				// }else{
				// String DBApartNum = proUtil.getValue("DBApartNum");
				// if(DBApartNum==null || DBApartNum.length()<1){
				// System.out.println("db.properties的配置出错！没有DBApartNum的值！");
				// return null;
				// }
				// int num = Integer.parseInt(DBApartNum);
				// List list = new ArrayList();
				// for(int i=1; i<=num; i++){ //循环查找每个分库
				// CustomerContextHolder cch = new CustomerContextHolder();
				// cch.setDynamicDataSource(String.valueOf(i));
				// list_result =
				// getActivityPersonService().getByTestStatusAndTime(pro[1],
				// activityId, beginDate, endDate);
				// if(list_result!=null && list_result.size()>0){
				// list.addAll(list_result);
				// }
				// new CustomerContextHolder().setDynamicDataSource("0");
				// //还原成主库
				// }
				// return list;
				// }
				Long toolId = Long.parseLong(pro[1]);
				if (apList == null || apList.size() < 1) {
					return null;
				}
				int i = 0;
				for (ActivityPersons ap : apList) {
					i++;
					System.out.println("export score:"
							+ i
							+ "/"
							+ apList.size()
							+ " checking persons......"
							+ (Calendar.getInstance().getTimeInMillis() - begin
									.getTime()));
					// 分库取数据
					String dbid = "";
					CustomerContextHolder cch = new CustomerContextHolder();
					dbid = String
							.valueOf(cch.getDbIdByseqCode(ap.getSeqCode()));
					if (!"1".equals(dbid)) {
						cch.setDynamicDataSource(dbid);
						// add by zw 20161009
						// 切换分库后首次aptService查询未指向分库，第二次则指向分库，原因不详，所以临时特殊处理这个bug
						aptService.getById(0l);
					}
					ActivityPersonTools apt = aptService.getByActivityPersonId(
							ap.getId(), toolId);
					if (apt == null) {
						new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
						continue;
					}
					if (apt.getTestStatus() == 2) {
						if ("".equals(beginDate) && "".equals(endDate)) {
							list_result.add(ap);
						} else if ("".equals(beginDate) && !"".equals(endDate)) {
							Date date = sdf.parse(endDate);
							if (!date.before(apt.getCompleteDate())) {
								list_result.add(ap);
							}
						} else if (!"".equals(beginDate) && "".equals(endDate)) {
							Date date = sdf.parse(beginDate);
							if (!date.after(apt.getCompleteDate())) {
								list_result.add(ap);
							}
						} else {
							Date bDate = sdf.parse(beginDate);
							Date eDate = sdf.parse(endDate);
							if (!bDate.after(apt.getCompleteDate())
									&& !eDate.before(apt.getCompleteDate())) {
								list_result.add(ap);
							}
						}
					}
					new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
				}

			} else if ("s".equals(pro[0])) {
				boolean flag2 = false;
				if (apList == null || apList.size() < 1) {
					return null;
				}
				Long solutionId = Long.parseLong(pro[1]);
				SolutionsToolsService stService = (SolutionsToolsService) Maxtech
						.getInstance().getBean(SolutionsToolsService.ID_NAME);
				List<SolutionsTools> stList = stService
						.getBySolutionId(solutionId);

				for (ActivityPersons ap : apList) {
					// 存放判断符合时间条件的结果
					List<String> erList1 = new ArrayList<String>();
					List<String> erList2 = new ArrayList<String>();

					// 分库取数据
					String dbid = "";
					CustomerContextHolder cch = new CustomerContextHolder();
					dbid = String
							.valueOf(cch.getDbIdByseqCode(ap.getSeqCode()));
					if (!"1".equals(dbid)) {
						cch.setDynamicDataSource(dbid);
					}

					for (SolutionsTools st : stList) {

						ActivityPersonTools apt = aptService
								.getByActivityPersonId(ap.getId(), st
										.getTools().getId());
						if (apt == null) {
							flag2 = false;
							break;
						} else {
							if (apt.getTestStatus() == 2) { // 筛选已经完成作答的
								flag2 = true;
								// 时间段筛选
								if (!"".equals(beginDate)) {
									Date date = sdf.parse(beginDate);
									erList1.add(String.valueOf(!date.after(apt
											.getCompleteDate())));
								} else {
									erList1.add("true");
								}

								if (!"".equals(endDate)) {
									Date date = sdf.parse(endDate);
									erList2.add(String.valueOf(!date.before(apt
											.getCompleteDate())));
								} else {
									erList2.add("true");
								}

							} else {
								flag2 = false;
								break;
							}
						}
					}
					new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
					if (flag2 && erList1.contains("true")
							&& erList2.contains("true")) {
						list_result.add(ap);
					}
				}
			}
			System.out.println("export score:"
					+ apList.size()
					+ " checking persons total times......"
					+ (Calendar.getInstance().getTimeInMillis() - begin
							.getTime()));
			return list_result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			new CustomerContextHolder().setDynamicDataSource("0"); // 还原成主库
		}
	}

	/**
	 * 人员根据产品扣费是否成功
	 * 
	 * @Type:ActivityToolAction.java </p>
	 * @Description: </p>
	 * @param apIdList
	 * @param product
	 * @return
	 * @author baixf
	 * @date 2017年3月1日 下午3:52:32
	 */
	private boolean isChargingOk(List<Long> apIdList, String product) {
		if (apIdList.size() > 0) {

			// 检查扣费
			Map<String, String> proApIdsHasmap = new HashMap<String, String>();
			StringBuffer apIDs = new StringBuffer();
			for (int i = 0; i < apIdList.size(); i++) {
				apIDs.append(apIdList.get(i));
				if (i != (apIdList.size() - 1)) {
					apIDs.append(",");
				}
			}
			proApIdsHasmap.put(product, apIDs.toString());
			ProductsPersonsService ppService = (ProductsPersonsService) Maxtech
					.getInstance().getBean(ProductsPersonsService.ID_NAME);
			Map<String, String> downloadProduct = ppService
					.ischargeUpdate(proApIdsHasmap);
			if ("false".equals(downloadProduct.get(product))) {
				System.out.println("导出成绩扣费失败");
				// printErrorJson(response,"导出成绩扣费失败，请检查剩余账号情况！");
				return false;
			}
		}
		return true;
	}

}
