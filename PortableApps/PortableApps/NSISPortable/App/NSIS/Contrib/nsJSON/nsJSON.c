/*
	nsJSON NSIS plug-in by Stuart Welch <afrowuk@afrowsoft.co.uk>
	v1.1.1.0 - 21st November 2017
*/

#include <windows.h>
#include <Wininet.h>
#include "LinkedList.h"
#include "nsJSON.h"
#include "JSON.h"
#include "pluginapi.h"

HANDLE g_hInstance;
struct LinkedList* g_pList = NULL;

struct THREAD_PARAM
{
	struct JSON_NODE* pNode;
	struct JSON_NODE* pRootNode;
};

static void NodeDelete(struct LinkedListNode* pListNode)
{
	JSON_Delete(&((struct JSON_NODE*)pListNode->Value), NULL);
	pListNode->Value = NULL;
}

static UINT_PTR PluginCallback(enum NSPIM msg)
{
	if (msg == NSPIM_UNLOAD)
	{
		LinkedListDestroy(&g_pList, NodeDelete);
	}
	return 0;
}

BOOL WINAPI DllMain(HANDLE hInst, ULONG ul_reason_for_call, LPVOID lpReserved)
{
	g_hInstance = hInst;
	return TRUE;
}

static struct JSON_NODE* GetTreeNode(PTCHAR pszTree, BOOL bCreate)
{
	struct LinkedListNode* pListNode;
	
	if (!g_pList)
		g_pList = LinkedListCreate();

	pListNode = LinkedListGet(g_pList, pszTree, bCreate);

	if (bCreate && pListNode && !pListNode->Value)
		pListNode->Value = JSON_Create();

	return pListNode ? (struct JSON_NODE*)pListNode->Value : NULL;
}

static struct JSON_NODE* PopTreeNode(PTCHAR pszArg, BOOL bCreate, BOOL* pbCreated)
{
	struct LinkedListNode* pListNode = NULL;

	if (!g_pList)
		g_pList = LinkedListCreate();

	if (pbCreated)
		*pbCreated = FALSE;

	if (popstring(pszArg) == 0)
	{
		if (lstrcmpi(pszArg, TEXT("/tree")) == 0)
		{
			if (popstring(pszArg) == 0)
				pListNode = LinkedListGet(g_pList, pszArg, bCreate);
		}
		else
		{
			pushstring(pszArg);
			lstrcpy(pszArg, TEXT(""));
		}
	}
			
	if (!pListNode)
		pListNode = LinkedListGet(g_pList, NULL, bCreate);

	if (bCreate && pListNode && !pListNode->Value)
	{
		pListNode->Value = JSON_Create();
		if (pbCreated)
			*pbCreated = TRUE;
	}

	return pListNode ? (struct JSON_NODE*)pListNode->Value : NULL;
}

enum HttpWebRequestDataType
{
	WRDT_Default = 0,
	WRDT_JSON = 1,
	WRDT_Raw = 2,
};

static enum HttpWebRequestDataType GetHttpWebRequestDataType(struct JSON_NODE* pRootNode)
{
	PTCHAR pszDataType = JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("DataType"), FALSE), TEXT(""));
	if (lstrcmpi(pszDataType, TEXT("JSON")) == 0)
		return WRDT_JSON;
	if (lstrcmpi(pszDataType, TEXT("Raw")) == 0)
		return WRDT_Raw;
	return WRDT_Default;
}

#define _trim(c) (c == ' ' || c == '\f' || c == '\n' || c == '\r' || c == '\t' || c == '\v' || c == '.')

static void GetLastErrorMessage(PTCHAR pszErrorMessage, const PTCHAR szWin32Func, DWORD dwLastError)
{
	PTCHAR pszError;
	int cchLastError;

	if (dwLastError >= 12001 && dwLastError <= 12156)
		cchLastError = FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_HMODULE | FORMAT_MESSAGE_IGNORE_INSERTS, GetModuleHandleA("wininet.dll"), dwLastError, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), (LPTSTR)&pszError, 0, NULL);
	else
		cchLastError = FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS, NULL, dwLastError, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), (LPTSTR)&pszError, 0, NULL);

	if (cchLastError > 0)
	{
		int cchWin32Func = lstrlen(szWin32Func);
		lstrcpy(pszErrorMessage, szWin32Func);
		lstrcpy(pszErrorMessage + cchWin32Func, TEXT(": "));

		for (cchLastError--; cchLastError >= 0 && _trim(pszError[cchLastError]); cchLastError--)
			pszError[cchLastError] = '\0';
		lstrcpy(pszErrorMessage + cchWin32Func + 2, pszError);

		wsprintf(pszErrorMessage + cchWin32Func + 2 + cchLastError, TEXT(" (%lu)"), dwLastError);
	}

	if (pszError)
		LocalFree(pszError);
}

static void SetLastErrorNode(struct JSON_NODE* pNode, const PTCHAR szWin32Func, DWORD dwLastError)
{
	PTCHAR pszError = (PTCHAR)GlobalAlloc(GPTR, 16 * sizeof(TCHAR));
	int cchLastError;

	if (pszError)
	{
		wsprintf(pszError, TEXT("%lu"), dwLastError);
		JSON_SetEx(pNode, TEXT("ErrorCode"), FALSE, (PBYTE)pszError, JSF_NONE);
		GlobalFree(pszError);
	}

	if (dwLastError >= 12001 && dwLastError <= 12156)
		cchLastError = FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_HMODULE | FORMAT_MESSAGE_IGNORE_INSERTS, GetModuleHandleA("wininet.dll"), dwLastError, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), (LPTSTR)&pszError, 0, NULL);
	else
		cchLastError = FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS, NULL, dwLastError, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), (LPTSTR)&pszError, 0, NULL);

	if (cchLastError > 0)
	{
		int cchWin32Func = lstrlen(szWin32Func);
		PTCHAR pszErrorMessage = (PTCHAR)GlobalAlloc(GPTR, (cchLastError + cchWin32Func + 3) * sizeof(TCHAR));
		if (pszErrorMessage)
		{
			lstrcpy(pszErrorMessage, szWin32Func);
			lstrcpy(pszErrorMessage + cchWin32Func, TEXT(": "));

			for (cchLastError--; cchLastError >= 0 && _trim(pszError[cchLastError]); cchLastError--)
				pszError[cchLastError] = '\0';
			lstrcpy(pszErrorMessage + cchWin32Func + 2, pszError);
#ifdef UNICODE
			JSON_SetEx(pNode, TEXT("ErrorMessage"), FALSE, (PBYTE)pszErrorMessage, JSF_IS_RAW | JSF_IS_UNICODE);
#else
			JSON_SetEx(pNode, TEXT("ErrorMessage"), FALSE, (PBYTE)pszErrorMessage, JSF_IS_RAW);
#endif
			GlobalFree(pszErrorMessage);
		}
	}

	if (pszError)
		LocalFree(pszError);
}

static void DoHttpWebRequest(struct JSON_NODE* pNode, struct JSON_NODE* pRootNode)
{
	static PTCHAR accept[2] = { TEXT("*/*"), NULL };

	PTCHAR pszUrl = JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("Url"), FALSE), NULL);
	if (pszUrl)
	{
		PTCHAR pszAgentNode = JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("Agent"), FALSE), TEXT("nsJSON NSIS plug-in/1.0.x.x")),
			pszAccessType = JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("AccessType"), FALSE), NULL),
			pszProxyName = NULL,
			pszProxyBypass = NULL;
		struct JSON_NODE* pProxyNode = JSON_Get(pRootNode, TEXT("Proxy"), FALSE);
		BOOL bSuccess = FALSE;

		DWORD dwAccessType = INTERNET_OPEN_TYPE_DIRECT;
		if (pszAccessType)
		{
			if (lstrcmpi(pszAccessType, TEXT("PreConfig")) == 0)
			{
				dwAccessType = INTERNET_OPEN_TYPE_PRECONFIG;
			}
			else if (lstrcmpi(pszAccessType, TEXT("Proxy")) == 0)
			{
				if (pProxyNode)
				{
					pszProxyName = JSON_GetQuotedValue(JSON_Get(pProxyNode, TEXT("Server"), FALSE), NULL);
					if (pszProxyName)
					{
						dwAccessType = INTERNET_OPEN_TYPE_PROXY;
						pszProxyBypass = JSON_GetQuotedValue(JSON_Get(pProxyNode, TEXT("Bypass"), FALSE), NULL);
					}
				}
			}
		}

		HINTERNET hSession = InternetOpen(pszAgentNode, INTERNET_OPEN_TYPE_DIRECT, pszProxyName, pszProxyBypass, 0);
		if (hSession != NULL)
		{
			LPURL_COMPONENTS pstUrlComp = (LPURL_COMPONENTS)GlobalAlloc(GPTR, sizeof(URL_COMPONENTS));
			if (pstUrlComp != NULL)
			{
				struct JSON_NODE* pParamsNode = JSON_Get(pRootNode, TEXT("Params"), FALSE);
				BOOL bRawParams = pParamsNode && lstrcmpi(JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("ParamsType"), FALSE), TEXT("")), TEXT("Raw")) == 0;
				PTCHAR pszParams = pParamsNode ? (bRawParams ? JSON_GetQuotedValue(pParamsNode, TEXT("")) : JSON_SerializeAlloc(pParamsNode->pValue, FALSE, TRUE)) : NULL;

				if (pParamsNode && !pszParams)
				{
					SetLastErrorNode(pNode, TEXT("JSON_SerializeAlloc"), GetLastError());
				}
				else
				{
					pstUrlComp->dwStructSize = sizeof(URL_COMPONENTS);
					pstUrlComp->dwUrlPathLength = pstUrlComp->dwHostNameLength = lstrlen(pszUrl) + 1;

					if (pParamsNode && pszParams && *pszParams)
						pstUrlComp->dwUrlPathLength += lstrlen(pszParams) + 1;

					pstUrlComp->lpszUrlPath = (PTCHAR)GlobalAlloc(GPTR, pstUrlComp->dwUrlPathLength * sizeof(TCHAR));
					if (pstUrlComp->lpszUrlPath)
					{
						pstUrlComp->lpszHostName = (PTCHAR)GlobalAlloc(GPTR, pstUrlComp->dwHostNameLength * sizeof(TCHAR));
						if (pstUrlComp->lpszHostName)
						{
							if (InternetCrackUrl(pszUrl, 0, 0, pstUrlComp))
							{
								HINTERNET hConnect = InternetConnect(hSession, pstUrlComp->lpszHostName, pstUrlComp->nPort, NULL, NULL, INTERNET_SERVICE_HTTP, 0, 0);
								if (hConnect)
								{
									PTCHAR pszVerb = JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("Verb"), FALSE), TEXT("GET"));
									PTCHAR pszValue;
									HINTERNET hRequest;
									BOOL bEncoding;

									if (pParamsNode && pszParams && *pszParams)
									{
										lstrcpy(pstUrlComp->lpszUrlPath + pstUrlComp->dwUrlPathLength, TEXT("?"));
										lstrcpy(pstUrlComp->lpszUrlPath + pstUrlComp->dwUrlPathLength + 1, pszParams);
									}

									if (pProxyNode)
									{
										pszValue = JSON_GetQuotedValue(JSON_Get(pProxyNode, TEXT("Username"), FALSE), NULL);
										if (pszValue)
											InternetSetOption(hConnect, INTERNET_OPTION_PROXY_USERNAME, pszValue, lstrlen(pszValue));

										pszValue = JSON_GetQuotedValue(JSON_Get(pProxyNode, TEXT("Password"), FALSE), NULL);
										if (pszValue)
											InternetSetOption(hConnect, INTERNET_OPTION_PROXY_PASSWORD, pszValue, lstrlen(pszValue));
									}

									if (bEncoding = JSON_IsTrue(JSON_Get(pRootNode, TEXT("Decoding"), FALSE)))
									{
										InternetSetOption(hConnect, INTERNET_OPTION_HTTP_DECODING, &bEncoding, sizeof(bEncoding));
									}

									pszValue = JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("Username"), FALSE), NULL);
									if (pszValue)
									{
										InternetSetOption(hConnect, INTERNET_OPTION_USERNAME, pszValue, lstrlen(pszValue));
									}

									pszValue = JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("Password"), FALSE), NULL);
									if (pszValue)
									{
										InternetSetOption(hConnect, INTERNET_OPTION_PASSWORD, pszValue, lstrlen(pszValue));
									}

									pszValue = JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("ConnectTimeout"), FALSE), NULL);
									if (pszValue)
									{
										ULONG ulValue = myatou(pszValue);
										InternetSetOption(hConnect, INTERNET_OPTION_CONNECT_TIMEOUT, &ulValue, sizeof(ulValue));
									}

									pszValue = JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("SendTimeout"), FALSE), NULL);
									if (pszValue)
									{
										ULONG ulValue = myatou(pszValue);
										InternetSetOption(hConnect, INTERNET_OPTION_SEND_TIMEOUT, &ulValue, sizeof(ulValue));
									}

									pszValue = JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("ReceiveTimeout"), FALSE), NULL);
									if (pszValue)
									{
										ULONG ulValue = myatou(pszValue);
										InternetSetOption(hConnect, INTERNET_OPTION_RECEIVE_TIMEOUT, &ulValue, sizeof(ulValue));
									}

									hRequest = HttpOpenRequest(hConnect, pszVerb, pstUrlComp->lpszUrlPath, NULL, NULL, accept,
										INTERNET_FLAG_NO_CACHE_WRITE |
										INTERNET_FLAG_NO_COOKIES |
										INTERNET_FLAG_NO_UI |
										INTERNET_FLAG_RELOAD |
										INTERNET_FLAG_KEEP_CONNECTION |
										(pstUrlComp->nScheme == INTERNET_SCHEME_HTTPS ?
											INTERNET_FLAG_SECURE |
											INTERNET_FLAG_IGNORE_REDIRECT_TO_HTTP |
											INTERNET_FLAG_IGNORE_REDIRECT_TO_HTTPS : 0), 0);
									if (hRequest)
									{
										struct JSON_NODE* pDataNode = JSON_Get(pRootNode, TEXT("Data"), FALSE);
										PTCHAR pszDataEncoding = JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("DataEncoding"), FALSE), NULL);
										enum HttpWebRequestDataType eDataType = GetHttpWebRequestDataType(pRootNode);
										struct JSON_NODE* pHeadersNode = JSON_Get(pRootNode, TEXT("Headers"), FALSE);
										BOOL bRequestSent = FALSE;

										if (pHeadersNode)
										{
											if (pHeadersNode->eType == JNT_QUOTED_VALUE)
											{
												HttpAddRequestHeaders(hRequest, pHeadersNode->pszValue, -1L, 0);
											}

											if (bEncoding)
											{
												HttpAddRequestHeaders(hRequest, TEXT("Accept-Encoding: gzip,deflate\r\n"), -1L, HTTP_ADDREQ_FLAG_ADD);
											}

											if (lstrcmpi(pszVerb, TEXT("POST")) == 0)
											{
												if (eDataType == WRDT_JSON)
												{
													HttpAddRequestHeaders(hRequest, TEXT("Content-Type: application/json\r\n"), -1L, HTTP_ADDREQ_FLAG_ADD);
												}
												else
												{
													HttpAddRequestHeaders(hRequest, TEXT("Content-Type: application/x-www-form-urlencoded\r\n"), -1L, HTTP_ADDREQ_FLAG_ADD);
												}
											}

											if (pHeadersNode->eType == JNT_NODE)
											{
												pHeadersNode = pHeadersNode->pValue;
												while (pHeadersNode)
												{
													int cchHeader = lstrlen(pHeadersNode->pszKey) + lstrlen(pHeadersNode->pszValue) + 4;
													PTCHAR pszHeader = (PTCHAR)GlobalAlloc(GPTR, (cchHeader + 1) * sizeof(TCHAR));
													if (pszHeader)
													{
														lstrcpy(pszHeader, pHeadersNode->pszKey);
														lstrcat(pszHeader, TEXT(": "));
														lstrcat(pszHeader, pHeadersNode->pszValue);
														lstrcat(pszHeader, TEXT("\r\n"));
														HttpAddRequestHeaders(hRequest, pszHeader, cchHeader, HTTP_ADDREQ_FLAG_ADD | HTTP_ADDREQ_FLAG_REPLACE);
														GlobalFree(pszHeader);
													}

													pHeadersNode = pHeadersNode->pNext;
												}
											}
										}

										if (pDataNode)
										{
											PTCHAR pszBuffer;

											if (eDataType == WRDT_JSON || eDataType == WRDT_Default)
											{
												pszBuffer = JSON_SerializeAlloc(pDataNode->pValue, FALSE, eDataType == WRDT_Default);
												if (!pszBuffer)
												{
													SetLastErrorNode(pNode, TEXT("JSON_SerializeAlloc"), GetLastError());
												}
											}
											else
											{
												pszBuffer = JSON_GetQuotedValue(pDataNode, TEXT(""));
											}

											if (pszBuffer)
											{
#ifdef UNICODE
												if (pszDataEncoding && lstrcmpi(pszDataEncoding, TEXT("Unicode")) == 0)
												{
													bRequestSent = HttpSendRequest(hRequest, NULL, 0, pszBuffer, lstrlen(pszBuffer) * sizeof(TCHAR));
													if (!bRequestSent)
														SetLastErrorNode(pNode, TEXT("HttpSendRequest"), GetLastError());
												}
												else
												{
													int cchConverted = lstrlen(pszBuffer);
													PCHAR pszConverted = JSON_FromUnicode(pszBuffer, &cchConverted, CP_ACP);
													if (pszConverted)
													{
														bRequestSent = HttpSendRequest(hRequest, NULL, 0, pszConverted, cchConverted);
														if (!bRequestSent)
															SetLastErrorNode(pNode, TEXT("HttpSendRequest"), GetLastError());
														GlobalFree(pszConverted);
													}
												}
#else
												if (pszDataEncoding && lstrcmpi(pszDataEncoding, TEXT("Unicode")) == 0)
												{
													int cbUnicode = lstrlen(pszBuffer);
													PWCHAR pszUnicode = JSON_ToUnicode(pszBuffer, &cbUnicode);
													if (pszUnicode)
													{
														bRequestSent = HttpSendRequest(hRequest, NULL, 0, pszUnicode, cbUnicode);
														if (!bRequestSent)
															SetLastErrorNode(pNode, TEXT("HttpSendRequest"), GetLastError());
														GlobalFree(pszUnicode);
													}
												}
												else
												{
													bRequestSent = HttpSendRequest(hRequest, NULL, 0, pszBuffer, lstrlen(pszBuffer) * sizeof(TCHAR));
													if (!bRequestSent)
														SetLastErrorNode(pNode, TEXT("HttpSendRequest"), GetLastError());
												}
#endif

												GlobalFree(pszBuffer);
											}
										}
										else
										{
											bRequestSent = HttpSendRequest(hRequest, NULL, 0, NULL, 0);
											if (!bRequestSent)
												SetLastErrorNode(pNode, TEXT("HttpSendRequest"), GetLastError());
										}

										if (bRequestSent)
										{
											DWORD dwSize;
											PTCHAR pszStatusCode = NULL;

											if (InternetQueryDataAvailable(hRequest, &dwSize, 0, 0))
											{
												PBYTE pbResponseBuffer = (PBYTE)GlobalAlloc(GPTR, dwSize + 1);
												if (pbResponseBuffer)
												{
													DWORD dwBytesRead = 0;
													if (InternetReadFile(hRequest, pbResponseBuffer, dwSize, &dwBytesRead))
													{
														JSON_SetEx(pNode, TEXT("Output"), FALSE, pbResponseBuffer,
															(JSON_IsTrue(JSON_Get(pRootNode, TEXT("UnicodeOutput"), FALSE)) ? JSF_IS_UNICODE : 0) |
															(JSON_IsTrue(JSON_Get(pRootNode, TEXT("RawOutput"), FALSE)) ? JSF_IS_RAW : 0));
													}
													else
													{
														SetLastErrorNode(pNode, TEXT("InternetReadFile"), GetLastError());
													}

													GlobalFree(pbResponseBuffer);
												}
											}
											else
											{
												SetLastErrorNode(pNode, TEXT("InternetQueryDataAvailable"), GetLastError());
											}

											dwSize = 0;
											if (!HttpQueryInfo(hRequest, HTTP_QUERY_STATUS_CODE, pszStatusCode, &dwSize, NULL) && GetLastError() == ERROR_INSUFFICIENT_BUFFER)
											{
												pszStatusCode = GlobalAlloc(GPTR, (dwSize + 1) * sizeof(TCHAR));
												if (pszStatusCode)
												{
													if (HttpQueryInfo(hRequest, HTTP_QUERY_STATUS_CODE, pszStatusCode, &dwSize, NULL))
													{
#ifdef UNICODE
														JSON_SetEx(pNode, TEXT("StatusCode"), FALSE, (PBYTE)pszStatusCode, JSF_IS_UNICODE);
#else
														JSON_SetEx(pNode, TEXT("StatusCode"), FALSE, (PBYTE)pszStatusCode, JSF_NONE);
#endif
													}

													GlobalFree(pszStatusCode);
												}
											}
										}

										InternetCloseHandle(hRequest);
									}
									else
									{
										SetLastErrorNode(pNode, TEXT("HttpOpenRequest"), GetLastError());
									}

									InternetCloseHandle(hConnect);
								}
								else
								{
									SetLastErrorNode(pNode, TEXT("InternetConnect"), GetLastError());
								}
							}
							else
							{
								SetLastErrorNode(pNode, TEXT("InternetCrackUrl"), GetLastError());
							}

							GlobalFree(pstUrlComp->lpszHostName);
						}

						GlobalFree(pstUrlComp->lpszUrlPath);
					}

					if (pParamsNode && pszParams && !bRawParams)
						GlobalFree(pszParams);
				}

				GlobalFree(pstUrlComp);
			}

			InternetCloseHandle(hSession);
		}
		else
		{
			SetLastErrorNode(pNode, TEXT("InternetOpen"), GetLastError());
		}
	}
}

static DWORD WINAPI DoHttpWebRequestThreadProc(LPVOID lpParameter)
{
	struct THREAD_PARAM* ptp = (struct THREAD_PARAM*)lpParameter;
	DoHttpWebRequest(ptp->pNode, ptp->pRootNode);
	GlobalFree(ptp);
	return 0;
}

static void WriteInput(HANDLE hStdIn, struct JSON_NODE* pNode, BOOL bIsUnicode)
{
	DWORD dwBytesWritten;
#ifdef UNICODE
	if (bIsUnicode)
	{
		WriteFile(hStdIn, pNode->pszValue, lstrlen(pNode->pszValue) * sizeof(TCHAR), &dwBytesWritten, NULL);
	}
	else
	{
		int cchValue = lstrlen(pNode->pszValue);
		PCHAR pszConverted = JSON_FromUnicode(pNode->pszValue, &cchValue, CP_ACP);
		if (pszConverted)
		{
			WriteFile(hStdIn, pszConverted, cchValue * sizeof(TCHAR), &dwBytesWritten, NULL);
			GlobalFree(pszConverted);
		}
	}
#else
	if (bIsUnicode)
	{
		int cchValue = lstrlenA(pNode->pszValue);
		PWCHAR pwszConverted = JSON_ToUnicode(pNode->pszValue, &cchValue);
		if (pwszConverted)
		{
			WriteFile(hStdIn, pNode->pszValue, lstrlen(pNode->pszValue) * sizeof(TCHAR), &dwBytesWritten, NULL);
			GlobalFree(pwszConverted);
		}
	}
	else
	{
		WriteFile(hStdIn, pNode->pszValue, lstrlen(pNode->pszValue) * sizeof(TCHAR), &dwBytesWritten, NULL);
	}
#endif
}

static void DoCreateProcess(struct JSON_NODE* pNode, struct JSON_NODE* pRootNode)
{
#ifdef UNICODE
	JSON_SetEx(pNode, TEXT("Output"), FALSE, (PBYTE)TEXT(""), JSF_IS_UNICODE);
	JSON_SetEx(pNode, TEXT("ExitCode"), FALSE, (PBYTE)TEXT(""), JSF_IS_UNICODE);
#else
	JSON_SetEx(pNode, TEXT("Output"), FALSE, (PBYTE)TEXT(""), JSF_NONE);
	JSON_SetEx(pNode, TEXT("ExitCode"), FALSE, (PBYTE)TEXT(""), JSF_NONE);
#endif

	PTCHAR pszPath = JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("Path"), FALSE), NULL);
	if (pszPath)
	{
		HANDLE hStdInRead = NULL, hStdInWrite = NULL, hStdOutRead = NULL, hStdOutWrite = NULL;
		PBYTE pbOutputBuffer = (PBYTE)GlobalAlloc(GPTR, g_stringsize);
		SECURITY_ATTRIBUTES saAttr;
		saAttr.nLength = sizeof(SECURITY_ATTRIBUTES);
		saAttr.bInheritHandle = TRUE;
		saAttr.lpSecurityDescriptor = NULL;

		if (pbOutputBuffer)
		{
			if (CreatePipe(&hStdOutRead, &hStdOutWrite, &saAttr, 0) &&
				SetHandleInformation(hStdOutRead, HANDLE_FLAG_INHERIT, 0) &&
				CreatePipe(&hStdInRead, &hStdInWrite, &saAttr, 0) &&
				SetHandleInformation(hStdInWrite, HANDLE_FLAG_INHERIT, 0))
			{
				PPROCESS_INFORMATION ppiProcInfo = (PPROCESS_INFORMATION)GlobalAlloc(GPTR, sizeof(PROCESS_INFORMATION));
				if (ppiProcInfo)
				{
					LPSTARTUPINFO psiStartInfo = (LPSTARTUPINFO)GlobalAlloc(GPTR, sizeof(STARTUPINFO));
					if (psiStartInfo)
					{
						struct JSON_NODE* pArgumentsNode = JSON_Get(pRootNode, TEXT("Arguments"), FALSE);
						PTCHAR pszArguments = NULL;

						if (pArgumentsNode)
						{
							if (pArgumentsNode->eType == JNT_VALUE || pArgumentsNode->eType == JNT_QUOTED_VALUE)
							{
								pszArguments = pArgumentsNode->pszValue;
							}
							else if (pArgumentsNode->eType == JNT_ARRAY)
							{
								DWORD cchAlloc = 0;
								struct JSON_NODE* pArrayNext = pArgumentsNode->pValue;
								while (pArrayNext)
								{
									if (pArrayNext->eType == JNT_VALUE || pArrayNext->eType == JNT_QUOTED_VALUE)
										cchAlloc += lstrlen(pArrayNext->pszValue) + 1;
									pArrayNext = pArrayNext->pNext;
								}

								if (cchAlloc > 0)
								{
									pszArguments = GlobalAlloc(GPTR, sizeof(TCHAR) * cchAlloc);
									if (pszArguments)
									{
										DWORD dwPos = 0;

										pArrayNext = pArgumentsNode->pValue;
										while (pArrayNext)
										{
											if (pArrayNext->eType == JNT_VALUE || pArrayNext->eType == JNT_QUOTED_VALUE)
											{
												lstrcpy(pszArguments + dwPos, pArrayNext->pszValue);
												dwPos += lstrlen(pArrayNext->pszValue);
												lstrcpy(pszArguments + dwPos, TEXT(" "));
												dwPos++;
											}
											pArrayNext = pArrayNext->pNext;
										}
									}
								}
							}
						}

						psiStartInfo->cb = sizeof(STARTUPINFO);
						psiStartInfo->hStdError = hStdOutWrite;
						psiStartInfo->hStdOutput = hStdOutWrite;
						psiStartInfo->hStdInput = hStdInRead;
						psiStartInfo->dwFlags = STARTF_USESTDHANDLES;

						if (CreateProcess(pszPath, pszArguments, NULL, NULL, TRUE, CREATE_NO_WINDOW, NULL, JSON_GetQuotedValue(JSON_Get(pRootNode, TEXT("WorkingDir"), FALSE), NULL), psiStartInfo, ppiProcInfo))
						{
							struct JSON_NODE* pInputNode = JSON_Get(pRootNode, TEXT("Input"), FALSE);
							DWORD dwBytesRead, dwTotalBytesRead = 0, dwValueBufferPos = 0;
							PBYTE pbValueBuffer = NULL;

							CloseHandle(ppiProcInfo->hThread);

							if (pInputNode)
							{
								BOOL bIsUnicode = JSON_IsTrue(JSON_Get(pRootNode, TEXT("UnicodeInput"), FALSE));

								if (pInputNode->eType == JNT_VALUE || pInputNode->eType == JNT_QUOTED_VALUE)
								{
									WriteInput(hStdInWrite, pInputNode, bIsUnicode);
								}
								else if (pInputNode->eType == JNT_ARRAY)
								{
									struct JSON_NODE* pArrayNext = pArgumentsNode->pValue;
									while (pArrayNext)
									{
										if (pArrayNext->eType == JNT_VALUE || pArrayNext->eType == JNT_QUOTED_VALUE)
											WriteInput(hStdInWrite, pArrayNext, bIsUnicode);
										pArrayNext = pArrayNext->pNext;
									}
								}
							}

							CloseHandle(hStdInWrite);
							hStdInWrite = NULL;
							CloseHandle(hStdOutWrite);
							hStdOutWrite = NULL;

							while (ReadFile(hStdOutRead, pbOutputBuffer, g_stringsize, &dwBytesRead, NULL) && dwBytesRead != 0)
							{
								DWORD i;

								dwTotalBytesRead += dwBytesRead;

								if (pbValueBuffer)
								{
									pbValueBuffer = (PBYTE)GlobalReAlloc(pbValueBuffer, dwTotalBytesRead + sizeof(TCHAR), GMEM_ZEROINIT | GMEM_MOVEABLE);
								}
								else
								{
									pbValueBuffer = (PBYTE)GlobalAlloc(GPTR, dwBytesRead + sizeof(TCHAR));
								}

								if (!pbValueBuffer)
								{
									SetLastErrorNode(pNode, TEXT("DoCreateProcess"), GetLastError());
									break;
								}

								for (i = 0; i < dwBytesRead; i++, dwValueBufferPos++)
								{
									pbValueBuffer[dwValueBufferPos] = pbOutputBuffer[i];
								}
							}

							if (pbValueBuffer)
							{
								JSON_SetEx(pNode, TEXT("Output"), FALSE, pbValueBuffer,
										(JSON_IsTrue(JSON_Get(pRootNode, TEXT("UnicodeOutput"), FALSE)) ? JSF_IS_UNICODE : 0) |
										(JSON_IsTrue(JSON_Get(pRootNode, TEXT("RawOutput"), FALSE)) ? JSF_IS_RAW : 0));
							}

							if (GetExitCodeProcess(ppiProcInfo->hProcess, &dwBytesRead))
							{
								PTCHAR pszExitCode = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * 11);
								if (pszExitCode)
								{
									wsprintf(pszExitCode, TEXT("%lu"), dwBytesRead);
#ifdef UNICODE
									JSON_SetEx(pNode, TEXT("ExitCode"), FALSE, (PBYTE)pszExitCode, JSF_IS_UNICODE);
#else
									JSON_SetEx(pNode, TEXT("ExitCode"), FALSE, (PBYTE)pszExitCode, JSF_NONE);
#endif
									GlobalFree(pszExitCode);
								}
							}

							CloseHandle(ppiProcInfo->hProcess);
						}

						if (pArgumentsNode && pArgumentsNode->eType == JNT_ARRAY && pszArguments)
							GlobalFree(pszArguments);

						GlobalFree(psiStartInfo);
					}

					GlobalFree(ppiProcInfo);
				}
			}

			if (hStdInWrite != NULL)
				CloseHandle(hStdInWrite);
			if (hStdOutWrite != NULL)
				CloseHandle(hStdOutWrite);
			if (hStdInRead != NULL)
				CloseHandle(hStdInRead);
			if (hStdOutRead != NULL)
				CloseHandle(hStdOutRead);

			GlobalFree(pbOutputBuffer);
		}
	}
}

static DWORD WINAPI DoCreateProcessThreadProc(LPVOID lpParameter)
{
	struct THREAD_PARAM* ptp = (struct THREAD_PARAM*)lpParameter;
	DoCreateProcess(ptp->pNode, ptp->pRootNode);
	GlobalFree(ptp);
	return 0;
}

static BOOL CallFunc(LPTHREAD_START_ROUTINE func, struct JSON_NODE* pNode, PTCHAR pszTree)
{
	struct JSON_NODE* pRootNode = GetTreeNode(pszTree, FALSE);
	BOOL bSuccess = FALSE;

	if (pRootNode)
	{
		struct THREAD_PARAM* ptp = (struct THREAD_PARAM*)GlobalAlloc(GPTR, sizeof(struct THREAD_PARAM));
		if (ptp)
		{
			ptp->pNode = pNode;
			ptp->pRootNode = pRootNode;

			if (JSON_IsTrue(JSON_Get(pRootNode, TEXT("Async"), FALSE)))
			{
				HANDLE hThread = CreateThread(NULL, 0, func, ptp, 0, NULL);
				if (hThread)
				{
					PTCHAR pszHandle = (PTCHAR)GlobalAlloc(GPTR, g_stringsize * sizeof(TCHAR));
					if (pszHandle)
					{
#ifdef _WIN64
						wsprintf(pszHandle, TEXT("%Id"), hThread);
#else
						wsprintf(pszHandle, TEXT("%d"), hThread);
#endif

#ifdef UNICODE
						JSON_SetEx(pRootNode, TEXT("Handle"), FALSE, (PBYTE)pszHandle, JSF_IS_UNICODE);
#else
						JSON_SetEx(pRootNode, TEXT("Handle"), FALSE, (PBYTE)pszHandle, JSF_NONE);
#endif
						GlobalFree(pszHandle);
						bSuccess = TRUE;
					}
				}
				else
				{
					GlobalFree(ptp);
				}
			}
			else if (JSON_IsTrue(JSON_Get(pRootNode, TEXT("UIAsync"), FALSE)))
			{
				HANDLE hThread = CreateThread(NULL, 0, func, ptp, 0, NULL);
				if (hThread)
				{
					BOOL bLoop = TRUE;
					while (bLoop)
					{
						MSG msg;
						if (MsgWaitForMultipleObjectsEx(1, &hThread, INFINITE, QS_ALLINPUT | QS_ALLPOSTMESSAGE, 0) != WAIT_OBJECT_0 + 1)
							break;

						while (bLoop && PeekMessage(&msg, NULL, 0, 0, PM_REMOVE))
						{
							if (msg.message == WM_QUIT)
							{
								PostMessage(msg.hwnd, msg.message, msg.wParam, msg.lParam);
								bLoop = FALSE;
							}
							else
							{
								TranslateMessage(&msg);
								DispatchMessage(&msg);
							}
						}
					}

					CloseHandle(hThread);
					bSuccess = TRUE;
				}
			}
			else
			{
				func(ptp);
				bSuccess = TRUE;
			}
		}
	}

	return bSuccess;
}

NSISFUNC(Serialize)
{
	DLL_INIT();
	{
		BOOL bOK = FALSE;

		PTCHAR pszArg = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * string_size);
		if (pszArg)
		{
			BOOL bIsFile = FALSE, bAsUnicode = FALSE, bFormat = FALSE;
			struct JSON_NODE* pNode = PopTreeNode(pszArg, FALSE, NULL);

			while (popstring(pszArg) == 0)
			{
				if (lstrcmpi(pszArg, TEXT("/file")) == 0)
				{
					bIsFile = TRUE;
					continue;
				}

				if (lstrcmpi(pszArg, TEXT("/unicode")) == 0)
				{
					bAsUnicode = TRUE;
					continue;
				}

				if (lstrcmpi(pszArg, TEXT("/format")) == 0)
				{
					bFormat = TRUE;
					continue;
				}

				if (!bIsFile)
					pushstring(pszArg);
				break;
			}

			if (pNode)
			{
#ifdef UNICODE
				if (!bIsFile)
					bAsUnicode = TRUE;
#endif
				if (JSON_Serialize(pNode, pszArg, string_size - 1, bIsFile, bAsUnicode, bFormat))
				{
					if (!bIsFile)
						pushstring(pszArg);
					bOK = TRUE;
				}
				else
				{
					GetLastErrorMessage(pszArg, TEXT("JSON_Serialize"), GetLastError());
					pushstring(pszArg);
				}
			}

			GlobalFree(pszArg);
		}
    
		if (!bOK)
			extra->exec_flags->exec_error = 1;
	}
}

static void PushKeys(struct JSON_NODE* pNode)
{
	if (pNode->pNext)
		PushKeys(pNode->pNext);

	if (pNode->pszKey)
		pushstring(pNode->pszKey);
	else
		pushstring(pNode->pszValue);
}

#define GET_ACTION_NOEXPAND 1
#define GET_ACTION_KEY 2
#define GET_ACTION_KEYS 3
#define GET_ACTION_TYPE 4
#define GET_ACTION_EXISTS 5
#define GET_ACTION_COUNT 6
#define GET_ACTION_ISEMPTY 7

NSISFUNC(Get)
{
	DLL_INIT();
	{
		BOOL bOK = FALSE;

		PTCHAR pszArg = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * string_size);
		if (pszArg)
		{
			struct JSON_NODE* pNode = PopTreeNode(pszArg, FALSE, NULL);
			struct JSON_NODE* pPrev;
			int nAction = 0;
			BOOL bKeyIsIndex = FALSE;
			      
			while (popstring(pszArg) == 0)
			{
				if (lstrcmpi(pszArg, TEXT("/noexpand")) == 0)
				{
					nAction = GET_ACTION_NOEXPAND;
					continue;
				}
        
				if (lstrcmpi(pszArg, TEXT("/key")) == 0)
				{
					nAction = GET_ACTION_KEY;
					continue;
				}

				if (lstrcmpi(pszArg, TEXT("/keys")) == 0)
				{
					nAction = GET_ACTION_KEYS;
					continue;
				}
        
				if (lstrcmpi(pszArg, TEXT("/type")) == 0)
				{
					nAction = GET_ACTION_TYPE;
					continue;
				}
        
				if (lstrcmpi(pszArg, TEXT("/exists")) == 0)
				{
					nAction = GET_ACTION_EXISTS;
					continue;
				}
        
				if (lstrcmpi(pszArg, TEXT("/count")) == 0)
				{
					nAction = GET_ACTION_COUNT;
					continue;
				}
        
				if (lstrcmpi(pszArg, TEXT("/isempty")) == 0)
				{
					nAction = GET_ACTION_ISEMPTY;
					continue;
				}

				pushstring(pszArg);
				break;
			}

			while (popstring(pszArg) == 0)
			{
				if (lstrcmpi(pszArg, TEXT("/end")) == 0)
					break;
        
				if (lstrcmpi(pszArg, TEXT("/index")) == 0)
				{
					bKeyIsIndex = TRUE;
					continue;
				}

				if (pNode)
				{
					pPrev = pNode;
					pNode = JSON_Next(&pPrev, pszArg, bKeyIsIndex, FALSE, NULL);
					bKeyIsIndex = FALSE;
				}
			}

			if (pNode)
			{
				if (nAction == GET_ACTION_ISEMPTY)
				{
					pushstring(pNode->pValue ? TEXT("no") : TEXT("yes"));
				}
				else if (nAction == GET_ACTION_COUNT)
				{
					wsprintf(pszArg, TEXT("%d"), JSON_Count(pNode));
					pushstring(pszArg);
				}
				else if (nAction == GET_ACTION_EXISTS)
				{
					pushstring(TEXT("yes"));
				}
				else if (nAction == GET_ACTION_TYPE)
				{
					switch (pNode->eType)
					{
					case JNT_NODE:
						pushstring(TEXT("node"));
						break;
					case JNT_ARRAY:
						pushstring(TEXT("array"));
						break;
					case JNT_VALUE:
						pushstring(TEXT("value"));
						break;
					case JNT_QUOTED_VALUE:
						pushstring(TEXT("string"));
						break;
					}
				}
				else if (nAction == GET_ACTION_KEY)
				{
					if (pNode->pszKey)
						pushstring(pNode->pszKey);
					else
						pushstring(pNode->pszValue);
				}
				else if (nAction == GET_ACTION_KEYS)
				{
					if (pNode->eType == JNT_NODE)
					{
						PushKeys(pNode->pValue);
						wsprintf(pszArg, TEXT("%d"), JSON_Count(pNode));
						pushstring(pszArg);
					}
					else
					{
						pushstring(TEXT("0"));
					}
				}
				else if (pNode->eType == JNT_VALUE)
				{
					pushstring(pNode->pszValue);
				}
				else if (pNode->eType == JNT_QUOTED_VALUE)
				{
					if (nAction == GET_ACTION_NOEXPAND)
					{
						pushstring(pNode->pszValue);
					}
					else
					{
						PTCHAR pszExpanded = JSON_Expand(pNode);
						if (pszExpanded)
						{
							pushstring(pszExpanded);
							GlobalFree(pszExpanded);
						}
					}
				}
				else if (pNode->pValue)
				{
					if (JSON_Serialize(pNode->pValue, pszArg, string_size - 1, FALSE, FALSE, FALSE))
					{
						pushstring(pszArg);
						bOK = TRUE;
					}
					else
					{
						GetLastErrorMessage(pszArg, TEXT("JSON_Serialize"), GetLastError());
						pushstring(pszArg);
					}
				}
				else
				{
					pushstring(TEXT(""));
				}

				bOK = TRUE;
			}
			else if (nAction == GET_ACTION_EXISTS)
			{
				pushstring(TEXT("no"));
				bOK = TRUE;
			}
			else if (nAction == GET_ACTION_TYPE)
			{
				pushstring(TEXT(""));
				bOK = TRUE;
			}
			else if (nAction == GET_ACTION_KEYS)
			{
				pushstring(TEXT("0"));
				bOK = TRUE;
			}

			GlobalFree(pszArg);
		}
    
		if (!bOK)
			extra->exec_flags->exec_error = 1;
	}
}

NSISFUNC(Set)
{
	DLL_INIT();
	{
		BOOL bOK = FALSE;

		PTCHAR pszArg = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * string_size);
		if (pszArg)
		{
			PTCHAR pszTree = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * string_size);
			if (pszTree)
			{
				BOOL bCreated;
				struct JSON_NODE* pTreeNode = PopTreeNode(pszTree, TRUE, &bCreated);
				struct JSON_NODE* pNode = pTreeNode;
				struct JSON_NODE* pPrev = NULL;
				BOOL bKeyIsIndex = FALSE;

				while (popstring(pszArg) == 0)
				{
					if (lstrcmpi(pszArg, TEXT("/index")) == 0)
					{
						bKeyIsIndex = TRUE;
						continue;
					}

					if (lstrcmpi(pszArg, TEXT("/value")) == 0)
					{
						if (popstring(pszArg) == 0 && pNode)
						{
#ifdef UNICODE
							if (JSON_Set(pNode, (PBYTE)pszArg, JSF_IS_UNICODE))
#else
							if (JSON_Set(pNode, (PBYTE)pszArg, JSF_NONE))
#endif
								bOK = TRUE;
						}
						break;
					}

					if (lstrcmpi(pszArg, TEXT("/file")) == 0)
					{
						if (popstring(pszArg) == 0)
						{
							if (lstrcmpi(pszArg, TEXT("/unicode")) == 0)
							{
								if (popstring(pszArg) == 0 && pNode)
								{
									if (JSON_Set(pNode, (PBYTE)pszArg, JSF_IS_FILE | JSF_IS_UNICODE))
										bOK = TRUE;
								}
							}
							else
							{
								if (JSON_Set(pNode, (PBYTE)pszArg, JSF_IS_FILE))
									bOK = TRUE;
							}
						}
						break;
					}

					if (lstrcmpi(pszArg, TEXT("/http")) == 0)
					{
						if (popstring(pszArg) == 0 && pNode)
						{
							if (CallFunc(DoHttpWebRequestThreadProc, pNode, pszArg))
								bOK = TRUE;
						}
						break;
					}

					if (lstrcmpi(pszArg, TEXT("/exec")) == 0)
					{
						if (popstring(pszArg) == 0 && pNode)
						{
							if (CallFunc(DoCreateProcessThreadProc, pNode, pszArg))
								bOK = TRUE;
						}
						break;
					}

					if (pNode)
					{
						pPrev = pNode;
						pNode = JSON_Next(&pPrev, pszArg, bKeyIsIndex, TRUE, &bCreated);
						bKeyIsIndex = FALSE;
					}
				}

				if (!bOK && pNode && bCreated)
				{
					if (pNode == pTreeNode)
						LinkedListDelete(&g_pList, pszTree);
					JSON_Delete(&pNode, pPrev);
				}

				GlobalFree(pszTree);
			}

			GlobalFree(pszArg);
		}
    
		if (!bOK)
			extra->exec_flags->exec_error = 1;
	}
}

NSISFUNC(Delete)
{
	DLL_INIT();
	{
		BOOL bOK = FALSE;

		PTCHAR pszArg = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * string_size);
		if (pszArg)
		{
			PTCHAR pszTree = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * string_size);
			if (pszTree)
			{
				struct JSON_NODE* pTreeNode = PopTreeNode(pszTree, FALSE, NULL);
				struct JSON_NODE* pNode = pTreeNode;
				struct JSON_NODE* pPrev = NULL;
				BOOL bKeyIsIndex = FALSE;

				while (popstring(pszArg) == 0)
				{
					if (lstrcmpi(pszArg, TEXT("/end")) == 0)
						break;

					if (lstrcmpi(pszArg, TEXT("/index")) == 0)
					{
						bKeyIsIndex = TRUE;
						continue;
					}

					if (pNode)
					{
						pPrev = pNode;
						pNode = JSON_Next(&pPrev, pszArg, bKeyIsIndex, FALSE, NULL);
						bKeyIsIndex = FALSE;
					}
				}

				if (pNode)
				{
					if (pNode == pTreeNode)
						LinkedListDelete(&g_pList, pszTree);
					JSON_Delete(&pNode, pPrev);
					bOK = TRUE;
				}

				GlobalFree(pszTree);
			}

			GlobalFree(pszArg);
		}
    
		if (!bOK)
			extra->exec_flags->exec_error = 1;
	}
}

NSISFUNC(Quote)
{
	DLL_INIT();
	{
		BOOL bOK = FALSE;

		PTCHAR pszArg = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * string_size);
		if (pszArg)
		{
			enum JSON_ESCAPE_FLAGS eFlags = JEF_QUOTE;
							      
			while (popstring(pszArg) == 0)
			{
				if (lstrcmpi(pszArg, TEXT("/unicode")) == 0)
				{
					eFlags |= JEF_ESCAPE_UNICODE;
					continue;
				}
        
				if (lstrcmpi(pszArg, TEXT("/always")) == 0)
				{
					eFlags |= JEF_ALWAYS_QUOTE;
					continue;
				}
        
				pushstring(pszArg);
				break;
			}

			if (popstring(pszArg) == 0)
			{
				PTCHAR pszQuoted = JSON_Escape(pszArg, eFlags);
				if (pszQuoted)
				{
					pushstring(pszQuoted);
					GlobalFree(pszQuoted);
					bOK = TRUE;
				}
			}

			GlobalFree(pszArg);
		}
    
		if (!bOK)
			extra->exec_flags->exec_error = 1;
	}
}

NSISFUNC(Wait)
{
	DLL_INIT();
	{
		BOOL bOK = FALSE;

		PTCHAR pszArg = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * string_size);
		if (pszArg)
		{
			if (popstring(pszArg) == 0)
			{
				struct JSON_NODE* pNode = GetTreeNode(pszArg, FALSE);
				if (pNode)
				{
					struct JSON_NODE* pPrev = pNode;
					struct JSON_NODE* pHandleNode = JSON_Next(&pPrev, TEXT("Handle"), FALSE, FALSE, NULL);
					if (pHandleNode && pHandleNode->eType == JNT_VALUE)
					{
						HANDLE hThread = (HANDLE)nsishelper_str_to_ptr(pHandleNode->pszValue);
						if (hThread)
						{
							int nTimeout = -1;

							while (popstring(pszArg) == 0)
							{
								if (lstrcmpi(pszArg, TEXT("/timeout")) == 0)
								{
									if (popstring(pszArg) == 0)
										nTimeout = myatoi(pszArg);
									continue;
								}

								pushstring(pszArg);
								break;
							}

							if (nTimeout >= 0)
							{
								if (WaitForSingleObject(hThread, nTimeout) == WAIT_TIMEOUT)
								{
									pushstring(TEXT("wait"));
								}
								else
								{
									pushstring(TEXT(""));
									CloseHandle(hThread);
									JSON_Delete(&pHandleNode, pPrev);
								}
							}
							else
							{
								WaitForSingleObject(hThread, INFINITE);
								CloseHandle(hThread);
								JSON_Delete(&pHandleNode, pPrev);
							}

							bOK = TRUE;
						}
					}
				}
			}

			GlobalFree(pszArg);
		}

		if (!bOK)
			extra->exec_flags->exec_error = 1;
	}
}

NSISFUNC(Sort)
{
	DLL_INIT();
	{
		BOOL bOK = FALSE;

		PTCHAR pszArg = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * string_size);
		if (pszArg)
		{
			struct JSON_NODE* pNode = PopTreeNode(pszArg, FALSE, NULL);
			struct JSON_NODE* pPrev = NULL;
			BOOL bKeyIsIndex = FALSE;
			enum JSON_SORT_FLAGS eFlags = 0;

			while (popstring(pszArg) == 0)
			{
				if (lstrcmpi(pszArg, TEXT("/end")) == 0)
					break;

				if (lstrcmpi(pszArg, TEXT("/index")) == 0)
				{
					bKeyIsIndex = TRUE;
					continue;
				}

				if (lstrcmpi(pszArg, TEXT("/options")) == 0)
				{
					eFlags = (enum JSON_SORT_FLAGS)popint();
					continue;
				}

				if (pNode)
				{
					pPrev = pNode;
					pNode = JSON_Next(&pPrev, pszArg, bKeyIsIndex, FALSE, NULL);
					bKeyIsIndex = FALSE;
				}
			}

			if (pNode)
			{
				JSON_Sort(pNode, eFlags);
				bOK = TRUE;
			}

			GlobalFree(pszArg);
		}

		if (!bOK)
			extra->exec_flags->exec_error = 1;
	}
}