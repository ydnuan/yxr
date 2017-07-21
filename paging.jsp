<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<html>
  <head>
  <title></title>
</head>
  
  <script type="text/javascript">
  		
	function dataSelect(backdata){
		$("#Description").val("");
	 	$("#Pname").val("");
	 	$("#Page").val("1");
		$("#PageSize").val("10");
		$("#SelectTemp").val("0");
		$("#enterpriseToolsId").val(${enterpriseToolsId});
		//20170720
		$("upF").val("");
		$("nextF").val("");
		$("bottomPageNoF").val("");
		$("topPageNoF").val("");
		
		    var productName=$("#selectByPName").val();
		    var description=$("#selectByDescription").val();
		    $("#Description").val(description);
		 	$("#Pname").val(productName);
		    var pageSzies=backdata[backdata.length-1].pageSize;
		    $("#PageSize").val(pageSzies);
			var previouss=backdata[backdata.length-1].prePage;
			$("upF").val(previouss);
			var netPages=backdata[backdata.length-1].nextPage;
			$("nextF").val(netPages);
			var bottomPageNo=backdata[backdata.length-1].totalPage;
			$("bottomPageNoF").val(bottomPageNo);
			var topPageNo=1;
			$("topPageNoF").val(topPageNo);
			var totalPages=backdata[backdata.length-1].totalPage;
			var totalRecords=backdata[backdata.length-1].totalCount;
			var pageNo=backdata[backdata.length-1].pageNo;
			var url="setReportTemplate.do?method=toSetReportTemplate&productName="+productName+"&description="+description;
			if(productName!=""||description!=""){
				url=url+"&selectTemp=1";
				$("#SelectTemp").val("1");
			}
			var upUrl = url+"&page="+previouss+"&pageSize="+pageSzies;
			var nextUrl = url+"&page="+netPages+"&pageSize="+pageSzies;
			var bottomUrl= url+"&page="+bottomPageNo+"&pageSize="+pageSzies;
			var topPageNoUrl= url+"&page="+1+"&pageSize="+pageSzies;
			
			$("#totalPages").val(totalPages);
			$("#totalPages").html(totalPages);
			$("#totalRecords").val(totalRecords);
			$("#totalRecords").html(totalRecords);
			$("#everyPage").attr("value",pageSzies);
			//$("#everyPage").attr("onchange",'changePageSzies('+'\''+pageNo+'\''+')');
			$("#page").attr("value",pageNo);
			//$("#page").attr("onchange",'changePage('+'\''+pageSzies+'\''+')');
		 	
		  //  $("#up").attr("href",'javascript:ajaxSelectAll('+'\''+upUrl+'\''+')');
		    $("#up").attr("href",'javascript:upFunction('+'\''+previouss+','+pageSzies+'\''+')');
			$("#next").attr("href",'javascript:nextFunction('+'\''+netPages+','+pageSzies+'\''+')'); 
			$("#bottomPageNo").attr("href",'javascript:bottomFunction('+'\''+bottomPageNo+','+pageSzies+'\''+')');
			$("#topPageNo").attr("href",'javascript:topFunction('+'\''+1+','+pageSzies+'\''+')');  
			
	  }

//fy的附加代码
   function changePageSzies(a){
//	alert("a"+a);
	   var productName=$("#selectByPName").val();
	    var description=$("#selectByDescription").val();
		var url="setReportTemplate.do?method=toSetReportTemplate&productName="+productName+"&description="+description;
		 if(productName!=""||description!=""){
			 $("#Description").val(description);
			 	$("#Pname").val(productName);
			 url=url+"&selectTemp=1";
				$("#SelectTemp").val("1");
		 }/* else{
			 url="setReportTemplate.do?method=toSetReportTemplate";
		 } */
		var b=$('#everyPage').val();
		 alert(b);
			var page=$('#page').val();
			var totalRecords =$("#totalRecords").val();
			  var re = /^[1-9]+[0-9]*]*$/;
			var z=re.test(b);
			  alert(z);
			  if(re.test(b==false)){
	  			  b=1;
	  		  }
	  		  if(b>totalRecords){
	  			  b=1;
	  		  } 
			var s = Math.ceil(totalRecords/b);
	  		alert(s);
			if(page>s){
				$("#Page").val(s);
				$("#PageSize").val(b);
				url=url+"&page="+s+"&pageSize="+b;
//			ajaxSelectAll(url);
			ajaxSelectAll();
				//ajaxSelectAll('\''+url+'\'');
				return;
			}
			url=url+"&page="+a+"&pageSize="+b;
			$("#Page").val(a);
			$("#PageSize").val(b);
			//ajaxSelectAll(url);
			ajaxSelectAll();
		}
		function changePage(pageSize){
			//alert("pageSize"+pageSize);
			var productName=$("#selectByPName").val();
		    var description=$("#selectByDescription").val();
			var url="setReportTemplate.do?method=toSetReportTemplate&productName="+productName+"&description="+description;
			 if(productName!=""||description!=""){
				 url=url+"&selectTemp=1";
					$("#SelectTemp").val("1");
			 }/* else{
				 url="setReportTemplate.do?method=toSetReportTemplate";
			 } */
			var page=$('#page').val();
			var b=$('#everyPage').val(); 
			var totalPage=$("#totalPages").val();
			  var re = /^[1-9]+[0-9]*]*$/;
	  		  if(re.test(page)==false){
	  			page=1;
	  		  }
			if(parseInt(page)>parseInt(totalPage)){
				page=totalPage;
			} 
			url=url+"&page="+page+"&pageSize="+pageSize;
//			ajaxSelectAll(url);
			$("#Page").val(page);
			$("#PageSize").val(pageSize);
			ajaxSelectAll(url);
		} 
function upFunction(previouss,pageSzies){
	$("#Page").val(previouss);
	$("#PageSize").val(pageSzies);
	ajaxSelectAll();
}
function nextFunction(next,pageSzies){
	$("#Page").val(next);
	$("#PageSize").val(pageSzies);
	ajaxSelectAll();
}
function bottomFunction(bottomPageNo,pageSzies){
	$("#Page").val(bottomPageNo);
	$("#PageSize").val(pageSzies);
	ajaxSelectAll();
}
function topFunction(top,pageSzies){
	$("#Page").val(1);
	$("#PageSize").val(pageSzies);
	ajaxSelectAll();
}
  </script>
  <body>
  
            	<div class="zdrc-page">
				每页显示 <input style="width:20px;" type="text" name="" value=""  placeholder="" id="everyPage" onchange="changePageSzies(document.getElementById('page').value)"> 条
					<a id="topPageNo" href="#" class="btn" onclick="topFunction(top,pageSzies)">首 页</a> <a id="up" href="#" class="btn" onclick="upFunction(previouss,pageSzies)">上一页</a> 
					<a id="next" href="" class="btn" onclick="nextFunction(next,pageSzies)">下一页</a> 
					<a id="bottomPageNo" href="" class="btn" onclick="bottomFunction(bottomPageNo,pageSzies)">尾 页</a>
				第 <input style="width:20px;" id="page" type="text" name="" value="" placeholder="" onchange="changePage(document.getElementById('everyPage').value)"> 页
				&nbsp; &nbsp; 共 <span id="totalPages"></span> 页, <span id="totalRecords" ></span>条数据
				<!-- 测试<input value="" onchange="c();"/> -->
			</div>
        
  </body>
</html>
