function doQuery()
{
//alert('doQuery...');	
	if((document.getElementById('tf01').value!='')&&(document.getElementById('tf02').value!='')&&(document.getElementById('tf03').value!=''))
	{
		var q_str = 'reqType=doQuery';

		q_str = q_str+'&param01='+document.getElementById('tf01').value;
		q_str = q_str+'&param02='+document.getElementById('tf02').value;
		q_str = q_str+'&param03='+document.getElementById('tf03').value;
		doAjax('Booking',q_str,'doQuery_back','post',0);
	}else
	{
		alert('Please, fill all the search fields...');
	}
}

function doQuery_back(result)
{
alert('result:'+result);
}





