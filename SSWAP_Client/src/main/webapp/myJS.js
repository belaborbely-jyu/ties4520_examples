function doQuery()
{
alert('doQuery...');	
	if((document.getElementById('rc').value!=''))
	{
		var q_str = 'reqType=doQuery';
		q_str = q_str+'&serviceURL='+document.getElementById('rc').value;
		doAjax('SSWAPServiceMed',q_str,'doQuery_back','post',0);
	}else
	{
		alert('Please, fill the Service URL...');
	}
}

function doQuery_back(result)
{
alert('result:'+result);
}





