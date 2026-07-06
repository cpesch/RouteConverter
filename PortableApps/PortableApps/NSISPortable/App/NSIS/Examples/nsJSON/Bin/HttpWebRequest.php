<?php

// Dump it...
@file_put_contents('GET.txt', print_r($_GET, true));

// Data POSTed is JSON.
if (isset($_SERVER['CONTENT_TYPE']) && $_SERVER['CONTENT_TYPE'] == 'application/json')
{
	if (!($input = @file_get_contents("php://input")) || ($json = @json_decode(@utf8_encode($input))) === null)
	{
		echo json_encode(array('Error' => 'No input given', 'SentHeaders' => getallheaders()));
		exit;
	}

	// Dump it...
	@file_put_contents('POST.json', $input);

	// Do stuff with the POSTed JSON (read from HttpWebRequest->Data).
	//$json->glossary->title = "nsJSON NSIS plug-in";
}
// Data POSTed as key value pairs.
else
{
	// Dump it...
	@file_put_contents('POST.txt', print_r($_POST, true));

	//$myValue = $_POST['MyValue'];
}

// Now respond with JSON (downloaded into HttpWebResponse).
echo json_encode(array('FirstName' => 'Stuart', 'LastName' => 'Welch', 'NickName' => 'Afrow UK', 'Email' => 'afrowuk@afrowsoft.co.uk'));

?>