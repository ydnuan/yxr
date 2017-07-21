<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title></title>
<%@ include file="/include/taglibs.jsp"%>
<%@ include file="/include/jquery.jsp"%>
<link href="${cxp}/appjs/plugin/table/jquery.table.css" rel="stylesheet"
	type="text/css" />
<script type="text/javascript"
	src="${cxp}/appjs/plugin/table/jquery.table.js"></script>
<%--  <script src="<c:out value='${cxp }'/>/appjs/ajaxform.common.js"></script>  --%>
<script type="text/javascript"
	src="${cxp}/js/My97DatePicker/WdatePicker.js"></script>

</head>
<body>
	<%
		String id = request.getParameter("id");
		request.setAttribute("enterpriseToolsId", id);
	%>
<!-- 	action="setReportTemplate.do?method=setReportTemplate" method="post"
 -->	
 		<form id="noForm" style="display: none;">
			<input type="text" name="description" id="Description"/>
			<input type="text" name="productName" id="Pname"/>
			<input type="text" name="page" id="Page"/>
			<input type="text" name="pageSize" id="PageSize"/>
			<input type="text" name="id" id="enterpriseToolsId"/>
			<input type="text" name="selectTemp" id="SelectTemp"/>
		</form>
 		<form id="myForm">
		<div>
			<h2 class="underline">
				企业名称:<span id="enterpriseName"></span>
			</h2>
			产品名称：<span id="productName"></span>&nbsp;&nbsp;&nbsp;&nbsp;产品编号：<span
				id="productCode"></span> <br /> 
				产品模板名称<input id="selectByPName" type="text" name="" value="" /> &nbsp;&nbsp;&nbsp;&nbsp;
				 产品模板描述<input id="selectByDescription" type="text" name="" value="" />&nbsp;&nbsp;&nbsp;&nbsp; 
				<input type="button"  value="查询" class="button_big" onclick="selectBySome()"/>
			<table class="table">
				<thead>
					<tr id="myHead">
						<th width="80"><input type="checkbox" class="input-checkbox" id="checkall" alt="ids"
							type="checkbox" name="c_all"
							onClick="selectAll(this.form,this.checked,this.nextSibling)" />
							全选</th>
						<th>报告模板名称</th>
						<th>报告模板描述</th>
						<th>模板预览</th>
					</tr>
					
				</thead>
				<tbody id="tbody"></tbody>
				<tr >
				<td colspan="4" id="myHead">
				<%@include file="/include/paging.jsp"%>
				</td>
				</tr>
			</table>
		</div>
		<center style="margin-top: 30px">
			<input type="button" value="提  交" class="button_big" onclick="fu()"/>
		</center>
	</form>
	<script type="text/javascript">
		var enid=${enterpriseToolsId};
		var getEnterpriseId = "";
		var toolsId = "";
		//'setReportTemplate.do?method=toSetReportTemplate&id=${enterpriseToolsId}',name="subOk"
		$(function() {
			$("#Description").val("");
		 	$("#Pname").val("");
		 	$("#Page").val("1");
			$("#PageSize").val("10");
			$("#SelectTemp").val("0");
			$("#enterpriseToolsId").val(${enterpriseToolsId});
			/* var url="setReportTemplate.do?method=toSetReportTemplate"+"&page="+1+"&pageSize="+10; */
			ajaxSelectAll();
		});
		 function fu(){ 
			 var array= "";
			// alert("99");
			 var flag = false;
			 var reminder = false;
			var num= $('#tbody input[type="checkbox"]:checked').length;
			if(num==0){
				alert("至少勾选一条");
				return;
			}
			 // $("#tbody .choose").each(function(){
			  $('#tbody input[type="checkbox"]:checked').each(function(){
			  if($(this).attr("disabled")!=true){
				  reminder=true;				  
				 // alert("进入");
				if($(this).attr("checked")==true){
					flag = true;
					if(array==""){
						array=$(this).val();
					//	 alert("第一个打勾的值"+array);
					}else{
						array=array+','+$(this).val();
						// alert("第二个打勾的值"+array);

					} }
				}
				});
			  if(reminder != true){
				  alert("至少勾选一条");
			  }
				 if(flag != true){
					 return;
				 }
			  $.ajax({
					type : "post",
					url : "setReportTemplate.do?method=setReportTemplate",
					dataType : "json",
					data : {
						check:array,
						enterpriseId:getEnterpriseId,
					    toolsId :toolsId
					},
					error : function() {
						alert("错误信息");
					},
					success : function(data) {
						if(data[0].errorCode==0){
							alert("至少勾选一条数据");
							return false;
						}
						alert("success!");
					}
				});
			}
		 //"setReportTemplate.do?method=toSetReportTemplate"
				 //ajaxSelectAll(selectTemp,page,pageSize,productName,description)
		 function ajaxSelectAll(){
				$("#tbody").html("");
				//alert("111asd");
				$.ajax({
							type : "post",
 							url : "${cxp}/zd_manager/system/setReportTemplate.do?method=toSetReportTemplate", 
							dataType : "json",
							data : $('#noForm').serialize(),
							error : function() {
								alert("error出错了数据查到的可能是空：暂无数据");
							},
							success : function(data) {
								dataSelect(data);
								var flag = "暂无数据";//判断工具是否有报告模板
								var dataFlag = false;//判断企业是否有报告模板并且判断是否打勾
								 getEnterpriseId = data[data.length - 1].getEnterpriseId;
								 toolsId = data[data.length - 1].toolsId;
								 $("#enterpriseName").html(
											data[data.length - 1].enterpriseName);
									$("#productName").html(
											data[data.length - 1].productName);
									$("#productCode").html(
											data[data.length - 1].productCode);
								if (data[0].data == flag) {
									$("#tbody").append(
											"<tr>" + "<td colspan='4'>"
													+ data[0].data + "</td>"
													+ "</tr>");
								} else {
									if (data[data.length - 1].flag != dataFlag) {
										for (var i = 0; i < data.length - 1; i++) {
											if (data[i].flagUnion != dataFlag) {
												$("#tbody")
														.append(
																"<tr>"
																		+ "<td> <input class='input-checkbox' name='c_all' type='checkbox' checked value='"+data[i].id+"'/>"
																		+ "</td>"
																		+ "	<td>"
																		+ data[i].name
																		+ "</td>"
																		+ "<td>"
																		+ data[i].description
																		+ "</td>"
																		+ "<td>"
																		//<a class="btn" target="_blank" href="http://talent.zhiding.com.cn/tools/PPF.pdf">产品简介</a>
																		+"<a  target='_blank' href="+ data[i].templateUrl+">"+data[i].templateUrl+"</a>"
																		+ "</td>"
																		+ "</tr>");
											} 
											else if (data[i].flagUnion == dataFlag) {
											 
												$("#tbody")
														.append(
																"<tr>"
																	+ "<td>"
																	+	"<input class='choose' name='c_all' type='checkbox'  value='"+data[i].id+"'/>"
																	+ "</td>"
																	+ "<td>"
																	+ 	data[i].name
																	+ "</td>"
																	+ "<td>"
																	+ 	data[i].description
																	+ "</td>"
																	+ "<td>"
																	+"<a target='_blank' href="+ data[i].templateUrl+">"+data[i].templateUrl+"</a>"
																	+ "</td>"
															+ "</tr>");
											}
										}
									} 
								//移动到了这里data[data.length - 1].flag 
								if(data[data.length - 1].templateType==3){
								for(var i=0;i < data.length - 1; i++){
								 if(data[i].defaultFlag==dataFlag){
											$("#tbody")
													.append(
															"<tr>"
																	+ "<td> <input class='choose' name='c_all' type='checkbox' value='"+data[i].id+"'/>"
																	+ "</td>"
																	+ "	<td>"
																	+ data[i].name
																	+ "</td>"
																	+ "<td>"
																	+ data[i].description
																	+ "</td>"
																	+ "<td>"
																	+"<a  target='_blank' href="+ data[i].templateUrl+">"+data[i].templateUrl+"</a>"
																	+ "</td>"
																	+ "</tr>");
										} 
								 if(data[i].defaultFlag!=dataFlag) {
											$("#tbody")
											.append(
													"<tr>"
															+ "<td> <input class='choose' name='c_all' type='checkbox' checked  value='"+ data[i].id+"'/>"
															+ "</td>"
															+ "	<td>"
															+ data[i].name
															+ "</td>"
															+ "<td>"
															+ data[i].description
															+ "</td>"
															+ "<td>"
															+"<a  target='_blank' href="+ data[i].templateUrl+">"+data[i].templateUrl+"</a>"
															+ "</td>"
															+ "</tr>");
										}}}
								}
							}
						});
		 }
		 
		 function selectBySome(){
		 	$("#Description").val("");
		 	$("#Pname").val("");
		 	$("#Page").val("1");
			$("#PageSize").val("10");
			$("#SelectTemp").val("1");
			// alert("至少有一个不能为空");
			 var productName=$("#selectByPName").val();
			// alert(productName);
			 var description=$("#selectByDescription").val();
			// ajaxSelectAll(1,1,1,productName,description);
 			 var url="setReportTemplate.do?method=toSetReportTemplate&selectTemp=1";
			 /* if(productName==""&&description==""){
				 alert("至少选填一个");
			 } */
			 if(productName==""||description==""){
			 	if(productName==""){
			 		$("#Description").val(description);
				 }
				 if(description==""){
				 	$("#Pname").val(productName);
				 }
			 }
			 if(productName!=""&&description!=""){
			 	$("#Description").val(description);
			 	$("#Pname").val(productName);
			 }
			// alert(url);
			 ajaxSelectAll(); 
			 
		 }
			    
	</script>

</body>


</html>

