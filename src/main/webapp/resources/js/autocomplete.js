function getDatasourceUrl(e,t){datasourceUrl=e,this.servlet=t,offset=2}function getDatasourceUrlWithFixedDelay(e,t,u){getDatasourceUrl(e,t),offset=u}function split(e){return e.split(/,\s*/)}function extractLast(e){return split(e).pop()}function setInputValue(e,t){null!=document.getElementById(e)&&(null!=t?document.getElementById(e).value=t:document.getElementById(e).value)}var datasourceUrl,result,servlet,offset=2,id;$(function(){$(".autocomplete_js").bind("keydown",function(e){null!=datasourceUrl&&""!=datasourceUrl&&e.keyCode===$.ui.keyCode.TAB&&$(this).data("autocomplete").menu.active&&e.getPreventDefault()}).autocomplete({source:function(e,t){$.getJSON(servlet,{searchkeyword:e.term,datasource:datasourceUrl},function(e){t(result=e)})},minLength:0,messages:{noResults:"",results:function(){return""}},search:function(){return null!=datasourceUrl&&""!=datasourceUrl&&(!(extractLast(this.value).length<offset)&&void 0)},focus:function(){return!1},select:function(e,t){for(var u=this.id.split(":"),n="",a=0;a<u.length-1;a++)n=n+u[a]+":";return setInputValue(this.id,t.item.value),setInputValue(n+"identifier",t.item.id),setInputValue(n+"given",t.item.givenname),setInputValue(n+"family",t.item.family),setInputValue(n+"identifier",t.item.id),setInputValue(n+"organization:0:name",t.item.organization),setInputValue(n+"latitude",t.item.latitude),setInputValue(n+"longitude",t.item.longitude),!1}}).focus(function(){0==offset&&null!=datasourceUrl&&""!=datasourceUrl&&(this.value=" ",$(this).autocomplete("search"),this.value="",event.getPreventDefault())})});