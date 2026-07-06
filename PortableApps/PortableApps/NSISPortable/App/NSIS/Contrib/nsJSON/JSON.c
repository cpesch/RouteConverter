/*
	JSON parser & writer by Stuart Welch <afrowuk@afrowsoft.co.uk>
	v1.1.1.0 - 21st November 2017
*/

#include <windows.h>
#include "JSON.h"
#include "pluginapi.h"

#define IsWhitespace(c) c == TEXT(' ') || c == TEXT('\t') || c == TEXT('\r') || c == TEXT('\n')
#define ntohs(n) (((((unsigned short)(n) & 0xFF)) << 8) | (((unsigned short)(n) & 0xFF00) >> 8))
static TCHAR hex[] = TEXT("0123456789abcdef");

struct JSON_NODE* JSON_Create()
{
	return (struct JSON_NODE*)GlobalAlloc(GPTR, sizeof(struct JSON_NODE));
}

PTCHAR JSON_GetQuotedValue(struct JSON_NODE* pNode, const PTCHAR pszDefaultValue)
{
	return pNode && pNode->eType == JNT_QUOTED_VALUE ? pNode->pszValue : pszDefaultValue;
}

BOOL JSON_IsTrue(struct JSON_NODE* pNode)
{
	if (!pNode || !pNode->pszValue || lstrcmp(pNode->pszValue, TEXT("0")) == 0 || lstrcmpi(pNode->pszValue, TEXT("false")) == 0)
		return FALSE;
	return TRUE;
}

static void EatWhitespace(PTCHAR pszBuffer, int* piPos)
{
	while (IsWhitespace(pszBuffer[*piPos]))
		(*piPos)++;
}

static PTCHAR MakeCopy(PTCHAR pszCopyFrom, int iStart, int iEnd)
{
	PTCHAR pszCopy = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * (iEnd - iStart + 2));
	if (pszCopy)
	{
		int i = 0;
		for (i = 0; iStart <= iEnd; i++, iStart++)
			pszCopy[i] = pszCopyFrom[iStart];
	}

	return pszCopy;
}

static BOOL IsChar(PTCHAR pszBuffer, int* piPos, TCHAR chExpected)
{
	EatWhitespace(pszBuffer, piPos);

	if (pszBuffer[*piPos] != chExpected)
		return FALSE;

	return TRUE;
}

/*static BOOL IsNumeric(PTCHAR pszBuffer)
{
	int cch = lstrlen(pszBuffer), i;
	if (cch == 0)
		return FALSE;

	if (pszBuffer[0] == TEXT('0'))
	{
		if (cch == 1)
			return TRUE;
		return FALSE;
	}

	for (i = 0; i < cch; i++)
		if (pszBuffer[i] < TEXT('0') || pszBuffer[i] > TEXT('9'))
			return FALSE;

	return TRUE;
}*/

static BOOL EatChar(PTCHAR pszBuffer, int* piPos, TCHAR chExpected)
{
	if (!IsChar(pszBuffer, piPos, chExpected))
		return FALSE;

	(*piPos)++;
	return TRUE;
}

static BOOL IsEscaped(PTCHAR pszBuffer, int iPos)
{
	return iPos > 0 && pszBuffer[iPos - 1] == TEXT('\\') && !IsEscaped(pszBuffer, iPos - 1);
}

static enum JSON_WORD_TYPE EatWord(PTCHAR pszBuffer, int* piPos, PTCHAR* ppszWord)
{
	int iStart, iEnd;

	EatWhitespace(pszBuffer, piPos);

	iStart = *piPos;
	iEnd = 0;
	*ppszWord = NULL;

	if (pszBuffer[*piPos] == TEXT('"'))
	{
		iStart++;

		while (TRUE)
		{
			(*piPos)++;
			if (pszBuffer[*piPos] == TEXT('"') && !IsEscaped(pszBuffer, *piPos) || pszBuffer[*piPos] == 0)
			{
				iEnd = *piPos - 1;
				if (pszBuffer[*piPos] == TEXT('"'))
					(*piPos)++;
				break;
			}
		}

		*ppszWord = MakeCopy(pszBuffer, iStart, iEnd);
		return JWT_STRING;
	}

	while (TRUE)
	{
		if (pszBuffer[*piPos] == TEXT(':') || pszBuffer[*piPos] == TEXT(',') || pszBuffer[*piPos] == TEXT('}') || pszBuffer[*piPos] == TEXT(']') || IsWhitespace(pszBuffer[*piPos]) || pszBuffer[*piPos] == 0)
		{
			iEnd = *piPos - 1;
			break;
		}
		(*piPos)++;
	}

	if (iStart <= iEnd)
	{
		*ppszWord = MakeCopy(pszBuffer, iStart, iEnd);
		return JWT_OTHER;
	}

	return JWT_NONE;
}

static struct JSON_NODE* EatNode(PTCHAR pszBuffer, int* piPos, BOOL bIsValue);

static struct JSON_NODE* EatNodeArray(PTCHAR pszBuffer, int* piPos)
{
	struct JSON_NODE* pNode = EatNode(pszBuffer, piPos, TRUE);
	if (pNode && EatChar(pszBuffer, piPos, TEXT(',')))
		pNode->pNext = EatNodeArray(pszBuffer, piPos);
	return pNode;
}

static struct JSON_NODE* EatNode(PTCHAR pszBuffer, int* piPos, BOOL bIsValue)
{
	struct JSON_NODE* pNode = JSON_Create();

	if (pNode)
	{
		if (EatChar(pszBuffer, piPos, TEXT('{')))
		{
			pNode->eType = JNT_NODE;
			pNode->pValue = EatNode(pszBuffer, piPos, FALSE);
			if (!pNode->pValue || !EatChar(pszBuffer, piPos, TEXT('}')))
			{
				JSON_Delete(&pNode, NULL);
				return NULL;
			}
		}
		else if (EatChar(pszBuffer, piPos, TEXT('[')))
		{
			pNode->eType = JNT_ARRAY;
			pNode->pValue = EatNodeArray(pszBuffer, piPos);
			if (!pNode->pValue || !EatChar(pszBuffer, piPos, TEXT(']')))
			{
				JSON_Delete(&pNode, NULL);
				return NULL;
			}
		}
		else
		{
			PTCHAR pszValue = NULL;
			enum JSON_WORD_TYPE eType = EatWord(pszBuffer, piPos, &pszValue);

			if (pszValue)
			{
				switch (eType)
				{
				case JWT_STRING:
					pNode->eType = JNT_QUOTED_VALUE;
					break;
				case JWT_OTHER:
					pNode->eType = JNT_VALUE;
					break;
				}
    
				if (eType != JWT_NONE)
				{
					// Node is just a value.
					if (bIsValue)
					{
						pNode->pszValue = pszValue;
					}
					// Node is a key: value pair.
					else if (EatChar(pszBuffer, piPos, TEXT(':')))
					{
						struct JSON_NODE* pChildNode = EatNode(pszBuffer, piPos, TRUE);
						if (!pChildNode)
						{
							GlobalFree(pNode);
							return NULL;
						}

						pNode->eType = pChildNode->eType;
						pNode->pNext = pChildNode->pNext;
						pNode->pValue = pChildNode->pValue;
						GlobalFree(pChildNode);

						pNode->pszKey = pszValue;
					}
					// No key was given; use an empty string.
					else
					{
						pNode->pszKey = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR));
						pNode->pszValue = pszValue;
					}
				}
			}
			else
			{
				GlobalFree(pNode);
				return NULL;
			}
		}
    
		// Commas are allowed; eat next node.
		if (!bIsValue && EatChar(pszBuffer, piPos, TEXT(',')))
		{
			pNode->pNext = EatNode(pszBuffer, piPos, FALSE);
			if (pNode->pNext == NULL)
			{
				GlobalFree(pNode);
				return NULL;
			}
		}
	}

	return pNode;
}

static struct JSON_NODE* EatRoot(PTCHAR pszBuffer)
{
	int iPos = 0;
	return EatNode(pszBuffer, &iPos, FALSE);
}

static PTCHAR EscapeQuotes(PTCHAR pszStr)
{
	int nQuotes = 0, i, cchLen = lstrlen(pszStr);
	PTCHAR pszStrNew;
  
	for (i = 0; i < cchLen; i++)
		if (pszStr[i] == TEXT('"'))
			nQuotes++;

	pszStrNew = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * (cchLen + nQuotes + 1));
	if (pszStrNew)
	{
		int j;
    
		for (i = 0, j = 0; i < cchLen; i++, j++)
		{
			if (pszStr[i] == TEXT('"') && !IsEscaped(pszStr, i))
			{
				pszStrNew[j] = TEXT('\\');
				j++;
			}

			pszStrNew[j] = pszStr[i];
		}
	}
  
	return pszStrNew;
}

void JSON_Delete(struct JSON_NODE** ppNode, struct JSON_NODE* pPrev)
{
	if (ppNode && *ppNode)
	{
		struct JSON_NODE* pNext = *ppNode;

		if (pPrev)
		{
			if (pPrev->pNext == *ppNode)
				pPrev->pNext = (*ppNode)->pNext;

			if ((pPrev->eType == JNT_NODE || pPrev->eType == JNT_ARRAY) && pPrev->pValue == *ppNode)
				pPrev->pValue = (*ppNode)->pNext;
		}

		while (pNext)
		{
			struct JSON_NODE* pNextNext = pNext->pNext;

			if (pNext->pszKey)
				GlobalFree(pNext->pszKey);

			switch (pNext->eType)
			{
			case JNT_NODE:
			case JNT_ARRAY:

				if (pNext->pValue)
					JSON_Delete(&pNext->pValue, NULL);

				break;
			case JNT_VALUE:
			case JNT_QUOTED_VALUE:

				GlobalFree(pNext->pszValue);

				break;
			}

			GlobalFree(pNext);

			if (pNext == *ppNode)
				*ppNode = NULL;

			pNext = pNextNext;
		}
	}
}

static PCHAR HandleUTF8Input(PBYTE pbBuffer, int* pcbBuffer)
{
	if (pbBuffer[0] == 0xEF && pbBuffer[1] == 0xBB && pbBuffer[2] == 0xBF) // UTF-8 sig
	{
		if (pcbBuffer)
			*pcbBuffer -= 3;
		return (PCHAR)(pbBuffer + 3);
	}

	return (PCHAR)pbBuffer;
}

static PWCHAR HandleUTF16Input(PBYTE pbBuffer, int* pcchBuffer)
{
	if (pbBuffer[0] == 0xFE && pbBuffer[1] == 0xFF) // UTF-16BE BOM
	{
		int i;
		PWCHAR pwszBuffer = (PWCHAR)(pbBuffer + 2);
		for (i = 0; pwszBuffer[i] != L'\0'; i++)
			pwszBuffer[i] = ntohs(pwszBuffer[i]);
		if (pcchBuffer)
			*pcchBuffer -= 2;
		return pwszBuffer;
	}

	if (pbBuffer[0] == 0xFF && pbBuffer[1] == 0xFE && (pbBuffer[2] != 0x00 || pbBuffer[3] != 0x00)) // UTF-16LE BOM
	{
		if (pcchBuffer)
			*pcchBuffer -= 2;
		return (PWCHAR)(pbBuffer + 2);
	}

	return (PWCHAR)pbBuffer;
}

static struct JSON_NODE* Parse(PBYTE pbSource, enum JSON_SET_FLAGS eFlags)
{
	struct JSON_NODE* pNode = NULL;
	PBYTE pbBuffer = NULL;
	DWORD dwSize = 0;

	if (eFlags & JSF_IS_FILE)
	{
		HANDLE hFile = CreateFile((PTCHAR)pbSource, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
		if (hFile != INVALID_HANDLE_VALUE)
		{
			dwSize = GetFileSize(hFile, NULL);
			if (dwSize > 0)
			{
				pbBuffer = (PBYTE)GlobalAlloc(GPTR, dwSize + ((eFlags & JSF_IS_UNICODE) ? sizeof(WCHAR) : sizeof(CHAR)));
				if (pbBuffer)
					ReadFile(hFile, pbBuffer, dwSize, &dwSize, NULL);
			}
			CloseHandle(hFile);
		}
	}
	else
	{
		pbBuffer = pbSource;
		dwSize = (eFlags & JSF_IS_UNICODE) ? lstrlenW((PWCHAR)pbSource) : lstrlenA((PCHAR)pbSource);
	}

	if (pbBuffer)
	{
#ifdef UNICODE
		if (eFlags & JSF_IS_UNICODE)
		{
			pNode = EatRoot(HandleUTF16Input(pbBuffer, NULL));
		}
		else
		{
			int cbBuffer = dwSize;
			PWCHAR pwszBuffer = JSON_ToUnicode(HandleUTF8Input(pbBuffer, &cbBuffer), &cbBuffer);
			if (pwszBuffer)
			{
				pNode = EatRoot(pwszBuffer);
				GlobalFree(pwszBuffer);
			}
		}
#else
		if (eFlags & JSF_IS_UNICODE)
		{
			int cchBuffer = dwSize / sizeof(WCHAR);
			PCHAR pszBuffer = JSON_FromUnicode(HandleUTF16Input(pbBuffer, &cchBuffer), &cchBuffer, CP_ACP);
			if (pszBuffer)
			{
				pNode = EatRoot(pszBuffer);
				GlobalFree(pszBuffer);
			}
		}
		else
		{
			pNode = EatRoot(HandleUTF8Input(pbBuffer, NULL));
		}
#endif

		if (eFlags & JSF_IS_FILE)
			GlobalFree(pbBuffer);
	}

	return pNode;
}

int JSON_Count(struct JSON_NODE* pNode)
{
	int i = 0;

	if (pNode)
	{
		pNode = pNode->pValue;
		for (; pNode != NULL; i++)
			pNode = pNode->pNext;
	}

	return i;
}

struct JSON_NODE* JSON_Get(struct JSON_NODE* pNode, PTCHAR pszKey, BOOL bKeyIsIndex)
{
	return JSON_Next(&pNode, pszKey, bKeyIsIndex, FALSE, NULL);
}

struct JSON_NODE* JSON_GetEx(struct JSON_NODE* pNode, PTCHAR pszKey, BOOL bKeyIsIndex, BOOL bCreate, BOOL* pbCreated)
{
	return JSON_Next(&pNode, pszKey, bKeyIsIndex, bCreate, pbCreated);
}

struct JSON_NODE* JSON_Next(struct JSON_NODE** ppNode, PTCHAR pszKey, BOOL bKeyIsIndex, BOOL bCreate, BOOL* pbCreated)
{
	if (pbCreated)
		*pbCreated = FALSE;

	if (ppNode)
	{
		struct JSON_NODE* pParent = *ppNode;
		struct JSON_NODE* pNext = NULL;

		pszKey = EscapeQuotes(pszKey);
		if (pszKey)
		{
			// We can only get a child node if the parent is also a node or array.
			if (pParent->eType == JNT_NODE || pParent->eType == JNT_ARRAY)
			{
				pNext = pParent->pValue;
      
				// Get the child node by index.
				if (bKeyIsIndex)
				{
					int i, j = myatoi(pszKey);

					// Negative index?
					if (j < 0)
						j = JSON_Count(pParent) + j;

					for (i = 0; i < j && pNext != NULL; i++)
					{
						*ppNode = pNext;
						pNext = pNext->pNext;
					}
				}
				// Get the child node by key.
				else if (pParent->eType == JNT_NODE)
				{
					while (pNext != NULL && !(!pNext->pszKey && !*pszKey || lstrcmp(pNext->pszKey, pszKey) == 0))
					{
						*ppNode = pNext;
						pNext = pNext->pNext;
					}
				}
				// Seek to the end of the array if we're going to add a new node element.
				else if (bCreate)
				{
					while (pNext != NULL)
					{
						*ppNode = pNext;
						pNext = pNext->pNext;
					}
				}
				// Otherwise find an array element that matches.
				else
				{
					while (pNext != NULL)
					{
						if ((pNext->eType == JNT_VALUE || pNext->eType == JNT_QUOTED_VALUE) && lstrcmp(pNext->pszValue, pszKey) == 0)
							break;

						*ppNode = pNext;
						pNext = pNext->pNext;
					}
				}

				// No existing child node found and the caller wants to create one.
				if (!pNext && bCreate)
				{
					struct JSON_NODE* pNew = (struct JSON_NODE*)GlobalAlloc(GPTR, sizeof(struct JSON_NODE));
					pNew->eType = JNT_NODE;

					if (pbCreated)
						*pbCreated = TRUE;

					// Adding a new node to an array.
					if (pParent->eType == JNT_ARRAY && !bKeyIsIndex)
					{
						pNew->pValue = (struct JSON_NODE*)GlobalAlloc(GPTR, sizeof(struct JSON_NODE));
						pNew->pValue->eType = JNT_NODE;

						if (*pszKey)
						{
							pNew->pValue->pszKey = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * (lstrlen(pszKey) + 1));
							if (pNew->pValue->pszKey)
								lstrcpy(pNew->pValue->pszKey, pszKey);
						}

						if (*ppNode != pParent)
							(*ppNode)->pNext = pNew;
						else
							pParent->pValue = pNew;

						pNext = pNew->pValue;
					}
					// Change parent to an array.
					else if (pParent->eType == JNT_NODE && bKeyIsIndex)
					{
						pParent->eType = JNT_ARRAY;

						if (*ppNode != pParent)
							(*ppNode)->pNext = pNew;
						else
							pParent->pValue = pNew;

						pNext = pNew;
					}
					// Adding a new node to a node.
					else
					{
						if (!bKeyIsIndex)
						{
							if (*pszKey)
							{
								pNew->pszKey = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * (lstrlen(pszKey) + 1));
								if (pNew->pszKey)
									lstrcpy(pNew->pszKey, pszKey);
							}
							else
							{
								pNew->pszKey = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR));
							}
						}

						if (*ppNode != pParent)
							(*ppNode)->pNext = pNew;
						else
							pParent->pValue = pNew;

						pNext = pNew;
					}
				}
			}

			GlobalFree(pszKey);
		}

		return pNext;
	}

	return NULL;
}

BOOL JSON_Set(struct JSON_NODE* pNode, PBYTE pbValue, enum JSON_SET_FLAGS eFlags)
{
	if (pNode)
	{
		if (eFlags & JSF_IS_RAW)
		{
			// We are overwriting the node's value.
			switch (pNode->eType)
			{
			case JNT_NODE:
			case JNT_ARRAY:

				if (pNode->pValue)
					JSON_Delete(&pNode->pValue, NULL);

				break;
			case JNT_VALUE:
			case JNT_QUOTED_VALUE:

				if (pNode->pszValue)
					GlobalFree(pNode->pszValue);

				break;
			}

			pNode->eType = JNT_QUOTED_VALUE;

#ifdef UNICODE
			if (eFlags & JSF_IS_UNICODE)
			{
				pNode->pszValue = JSON_Escape((PWCHAR)pbValue, JEF_NONE);
			}
			else
			{
				int cchValue = lstrlenA((PCHAR)pbValue);
				PWCHAR pwszConverted = JSON_ToUnicode((PCHAR)pbValue, &cchValue);
				if (pwszConverted)
				{
					pNode->pszValue = JSON_Escape(pwszConverted, JEF_NONE);
					GlobalFree(pwszConverted);
				}
				else
				{
					pNode->pszValue = NULL;
				}
			}
#else
			if (eFlags & JSF_IS_UNICODE)
			{
				int cchValue = lstrlenW((PWCHAR)pbValue);
				PCHAR pszConverted = JSON_FromUnicode((PWCHAR)pbValue, &cchValue, CP_ACP);
				if (pszConverted)
				{
					pNode->pszValue = JSON_Escape(pszConverted, JEF_NONE);
					GlobalFree(pszConverted);
				}
				else
				{
					pNode->pszValue = NULL;
				}
			}
			else
			{
				pNode->pszValue = JSON_Escape((PCHAR)pbValue, JEF_NONE);
			}
#endif
		}
		else
		{
			struct JSON_NODE* pChildNode = Parse(pbValue, eFlags);
			if (!pChildNode)
				return FALSE;

			switch (pNode->eType)
			{
			case JNT_NODE:

				// We are overwriting the node's value.
				if (pNode->pValue)
					JSON_Delete(&pNode->pValue, NULL);

				switch (pChildNode->eType)
				{
				case JNT_NODE:
				case JNT_ARRAY:

					// Child node is a key: value pair; set it as the parent node's value.
					if (pChildNode->pszKey)
					{
						pNode->eType = JNT_NODE;
						pNode->pValue = pChildNode;
					}
					// Child node is a node without a key; overwrite the parent node's value with the child node's value.
					else
					{
						pNode->eType = pChildNode->eType;
						pNode->pValue = pChildNode->pValue;
						GlobalFree(pChildNode);
					}

					break;
				default:

					// Replace the node's value.
					pNode->eType = pChildNode->eType;
					pNode->pszValue = pChildNode->pszValue;
					GlobalFree(pChildNode);

					break;
				}

				break;
			case JNT_ARRAY:

				if (pNode->pValue)
				{
					struct JSON_NODE* pArrayNode = pNode->pValue;

					// Seek to the end of the array and then append to the end.
					while (TRUE)
					{
						if (pArrayNode->pNext == NULL)
							break;

						pArrayNode = pArrayNode->pNext;
					}

					pArrayNode->pNext = pChildNode;
				}
				// Replacing node.
				else
				{
					pNode->eType = pChildNode->eType;
					pNode->pValue = pChildNode->pValue;
					GlobalFree(pChildNode);
				}

				break;
			case JNT_VALUE:
			case JNT_QUOTED_VALUE:

				// We are overwriting the node's value. 
				if (pNode->pszValue)
					GlobalFree(pNode->pszValue);

				switch (pChildNode->eType)
				{
				case JNT_NODE:
				case JNT_ARRAY:

					// Child node is a key: value pair; set it as the parent node's value.
					if (pChildNode->pszKey)
					{
						pNode->eType = pChildNode->eType;
						pNode->pValue = pChildNode;
					}
					// Child node is a node without a key; overwrite the parent node's value with the child node's value.
					else
					{
						pNode->eType = pChildNode->eType;
						pNode->pValue = pChildNode->pValue;
						GlobalFree(pChildNode);
					}

					break;
				default:

					// Replace the node's value.
					pNode->eType = pChildNode->eType;
					pNode->pszValue = pChildNode->pszValue;
					GlobalFree(pChildNode);

					break;
				}

				break;
			}
		}
	}

	return TRUE;
}

BOOL JSON_SetEx(struct JSON_NODE* pNode, PTCHAR pszKey, BOOL bKeyIsIndex, PBYTE pbValue, enum JSON_SET_FLAGS eFlags)
{
	struct JSON_NODE* pNew = JSON_Next(&pNode, pszKey, bKeyIsIndex, TRUE, NULL);
	if (pNew)
		return JSON_Set(pNew, pbValue, eFlags);
	return FALSE;
}

static void MyWriteFile(HANDLE hFile, PTCHAR pszText, int cchText, BOOL bAsUnicode)
{
	DWORD dwBytes;

#ifdef UNICODE
	if (bAsUnicode)
	{
		WriteFile(hFile, pszText, sizeof(WCHAR) * cchText, &dwBytes, NULL);
	}
	else
	{
		PCHAR pszConverted = JSON_FromUnicode(pszText, &cchText, CP_ACP);
		if (pszConverted)
		{
			WriteFile(hFile, pszConverted, cchText, &dwBytes, NULL);
			GlobalFree(pszConverted);
		}
	}
#else
	if (bAsUnicode)
	{
		PWCHAR pwszConverted = JSON_ToUnicode(pszText, &cchText);
		if (pwszConverted)
		{
			WriteFile(hFile, pwszConverted, cchText, &dwBytes, NULL);
			GlobalFree(pwszConverted);
		}
	}
	else
	{
		WriteFile(hFile, pszText, cchText, &dwBytes, NULL);
	}
#endif
}

static void MyWriteFileEx(HANDLE hFile, PTCHAR pszText, int cchText, BOOL bAsUnicode, int iRepeat)
{
	int i = 0;

	for (; i < iRepeat; i++)
	{
		MyWriteFile(hFile, pszText, cchText, bAsUnicode);
	}
}

static void SerializeToFile(HANDLE hFile, struct JSON_NODE* pNode, int iIndent, BOOL bAsUnicode)
{
	if (!pNode)
		return;

	if (pNode->pszKey)
	{
		MyWriteFile(hFile, TEXT("\""), 1, bAsUnicode);
		MyWriteFile(hFile, pNode->pszKey, lstrlen(pNode->pszKey), bAsUnicode);
		MyWriteFile(hFile, TEXT("\":"), 2, bAsUnicode);

		if (iIndent)
		{
			MyWriteFile(hFile, TEXT(" "), 1, bAsUnicode);
		}
	}

	switch (pNode->eType)
	{
	case JNT_NODE:
	case JNT_ARRAY:
    
		MyWriteFile(hFile, pNode->eType == JNT_ARRAY ? TEXT("[") : TEXT("{"), 1, bAsUnicode);
    
		if (iIndent)
		{
			MyWriteFile(hFile, TEXT("\r\n"), 2, bAsUnicode);
			MyWriteFileEx(hFile, TEXT(JSON_INDENT_CHAR), 1, bAsUnicode, iIndent);
		}

		SerializeToFile(hFile, pNode->pValue, iIndent ? iIndent + JSON_INDENT : 0, bAsUnicode);
    
		if (iIndent)
		{
			MyWriteFile(hFile, TEXT("\r\n"), 2, bAsUnicode);
			MyWriteFileEx(hFile, TEXT(JSON_INDENT_CHAR), 1, bAsUnicode, iIndent - 1);
		}

		MyWriteFile(hFile, pNode->eType == JNT_ARRAY ? TEXT("]") : TEXT("}"), 1, bAsUnicode);

		break;
	case JNT_VALUE:

		MyWriteFile(hFile, pNode->pszValue, lstrlen(pNode->pszValue), bAsUnicode);

		break;
	case JNT_QUOTED_VALUE:
    
		MyWriteFile(hFile, TEXT("\""), 1, bAsUnicode);
		MyWriteFile(hFile, pNode->pszValue, lstrlen(pNode->pszValue), bAsUnicode);
		MyWriteFile(hFile, TEXT("\""), 1, bAsUnicode);
    
		break;
	}

	if (pNode->pNext)
	{
		pNode = pNode->pNext;
    
		MyWriteFile(hFile, TEXT(","), 1, bAsUnicode);
    
		if (iIndent)
		{
			MyWriteFile(hFile, TEXT("\r\n"), 2, bAsUnicode);
			MyWriteFileEx(hFile, TEXT(JSON_INDENT_CHAR), 1, bAsUnicode, iIndent - 1);
		}

		SerializeToFile(hFile, pNode, iIndent, bAsUnicode);
	}
}

static void MyStrCpy(PTCHAR* ppszBuffer, int* pcchBuffer, int* piPos, PTCHAR pszCopy, int cchCopyLen, BOOL bResize, DWORD* pdwLastError)
{
	if (*piPos + cchCopyLen <= *pcchBuffer)
	{
		if (ppszBuffer && *ppszBuffer)
			lstrcpy(*ppszBuffer + *piPos, pszCopy);
		*piPos += cchCopyLen;
	}
	else if (bResize)
	{
		*pcchBuffer *= 2;

		if (ppszBuffer && *ppszBuffer)
		{
			PTCHAR pszNewBuffer = (PTCHAR)GlobalReAlloc(*ppszBuffer, *pcchBuffer, GMEM_ZEROINIT | GMEM_MOVEABLE);
			if (!pszNewBuffer)
			{
				*pdwLastError = GetLastError();
				GlobalFree(*ppszBuffer);
				*ppszBuffer = NULL;
			}
			else
			{
				*ppszBuffer = pszNewBuffer;
			}
		}

		MyStrCpy(ppszBuffer, pcchBuffer, piPos, pszCopy, cchCopyLen, TRUE, pdwLastError);
	}
	else
	{
		int i = 0;
		for (; i < cchCopyLen && *piPos < *pcchBuffer; i++, (*piPos)++)
		{
			if (ppszBuffer && *ppszBuffer)
				(*ppszBuffer)[*piPos] = pszCopy[i];
		}
	}
}

static void MyStrCpyEx(PTCHAR* ppszBuffer, int* pcchBuffer, int* piPos, PTCHAR pszCopy, int cchCopyLen, int iRepeat, BOOL bResize, DWORD* pdwLastError)
{
	int i = 0;
	for (; i < iRepeat; i++)
	{
		MyStrCpy(ppszBuffer, pcchBuffer, piPos, pszCopy, cchCopyLen, bResize, pdwLastError);
	}
}

static void SerializeToBuffer(PTCHAR* ppszBuffer, int* pcchBuffer, struct JSON_NODE* pNode, int* piPos, int iIndent, BOOL bResize, DWORD* pdwLastError)
{
	if (!pNode)
	{
		if (ppszBuffer && *ppszBuffer)
			lstrcpy(*ppszBuffer + *piPos, TEXT(""));
		return;
	}

	if (pNode->pszKey)
	{
		MyStrCpy(ppszBuffer, pcchBuffer, piPos, TEXT("\""), 1, bResize, pdwLastError);
		MyStrCpy(ppszBuffer, pcchBuffer, piPos, pNode->pszKey, lstrlen(pNode->pszKey), bResize, pdwLastError);
		MyStrCpy(ppszBuffer, pcchBuffer, piPos, TEXT("\":"), 2, bResize, pdwLastError);

		if (iIndent)
		{
			MyStrCpy(ppszBuffer, pcchBuffer, piPos, TEXT(" "), 1, bResize, pdwLastError);
		}
	}

	switch (pNode->eType)
	{
	case JNT_NODE:
	case JNT_ARRAY:
    
		MyStrCpy(ppszBuffer, pcchBuffer, piPos, pNode->eType == JNT_ARRAY ? TEXT("[") : TEXT("{"), 1, bResize, pdwLastError);

		if (iIndent)
		{
			MyStrCpy(ppszBuffer, pcchBuffer, piPos, TEXT("\r\n"), 2, bResize, pdwLastError);
			MyStrCpyEx(ppszBuffer, pcchBuffer, piPos, TEXT(JSON_INDENT_CHAR), 1, iIndent, bResize, pdwLastError);
		}

		SerializeToBuffer(ppszBuffer, pcchBuffer, pNode->pValue, piPos, iIndent ? iIndent + JSON_INDENT : 0, bResize, pdwLastError);

		if (iIndent)
		{
			MyStrCpy(ppszBuffer, pcchBuffer, piPos, TEXT("\r\n"), 2, bResize, pdwLastError);
			MyStrCpyEx(ppszBuffer, pcchBuffer, piPos, TEXT(JSON_INDENT_CHAR), 1, iIndent - 1, bResize, pdwLastError);
		}

		MyStrCpy(ppszBuffer, pcchBuffer, piPos, pNode->eType == JNT_ARRAY ? TEXT("]") : TEXT("}"), 1, bResize, pdwLastError);

		break;
	case JNT_VALUE:
    
		MyStrCpy(ppszBuffer, pcchBuffer, piPos, pNode->pszValue, lstrlen(pNode->pszValue), bResize, pdwLastError);

		break;
	case JNT_QUOTED_VALUE:
    
		MyStrCpy(ppszBuffer, pcchBuffer, piPos,  TEXT("\""), 1, bResize, pdwLastError);
		MyStrCpy(ppszBuffer, pcchBuffer, piPos, pNode->pszValue, lstrlen(pNode->pszValue), bResize, pdwLastError);
		MyStrCpy(ppszBuffer, pcchBuffer, piPos,  TEXT("\""), 1, bResize, pdwLastError);
    
		break;
	}

	if (pNode->pNext)
	{
		pNode = pNode->pNext;
    
		MyStrCpy(ppszBuffer, pcchBuffer, piPos, TEXT(","), 1, bResize, pdwLastError);

		if (iIndent)
		{
			MyStrCpy(ppszBuffer, pcchBuffer, piPos, TEXT("\r\n"), 2, bResize, pdwLastError);
			MyStrCpyEx(ppszBuffer, pcchBuffer, piPos, TEXT(JSON_INDENT_CHAR), 1, iIndent - 1, bResize, pdwLastError);
		}

		SerializeToBuffer(ppszBuffer, pcchBuffer, pNode, piPos, iIndent, bResize, pdwLastError);
	}
}

static BOOL AddRoot(struct JSON_NODE** ppNode)
{
	struct JSON_NODE* pRoot = JSON_Create();
	if (pRoot)
	{
		pRoot->eType = JNT_NODE;
		pRoot->pValue = *ppNode;
		*ppNode = pRoot;
		return TRUE;
	}
	
	return FALSE;
}

static TCHAR i2a(TCHAR code)
{
	return hex[code & 15];
}
 
static int EscapePostData(PTCHAR* ppszPostData, int* pcchPostData, DWORD* pdwLastError)
{
	int cchLength = 0;
	if (ppszPostData && *ppszPostData)
	{
		PTCHAR pszBuffer = (PTCHAR)GlobalAlloc(GPTR, (lstrlen(*ppszPostData) * 3 + 1) * sizeof(TCHAR));

		if (pszBuffer)
		{
			PTCHAR pszBufferPtr = pszBuffer, pszPostDataPtr = *ppszPostData;
			int iPos = 0;

			while (*pszPostDataPtr)
			{
				if (IsCharAlphaNumeric(*pszPostDataPtr) || *pszPostDataPtr == TEXT('-') || *pszPostDataPtr == TEXT('_') || *pszPostDataPtr == TEXT('.') || *pszPostDataPtr == TEXT('~'))
				{
					*pszBufferPtr++ = *pszPostDataPtr;
				}
				else if (*pszPostDataPtr == TEXT(' '))
				{
					*pszBufferPtr++ = TEXT('+');
				}
				else
				{
					*pszBufferPtr++ = TEXT('%');
					*pszBufferPtr++ = i2a(*pszPostDataPtr >> 4);
					*pszBufferPtr++ = i2a(*pszPostDataPtr & 15);
				}

				pszPostDataPtr++;
			}

			if ((cchLength = lstrlen(pszBuffer)) > 0)
				MyStrCpy(ppszPostData, pcchPostData, &iPos, pszBuffer, cchLength, TRUE, pdwLastError);

			GlobalFree(pszBuffer);
		}
	}

	return cchLength;
}

BOOL JSON_Serialize(struct JSON_NODE* pNode, PTCHAR pszBuffer, int cchBuffer, BOOL bIsFile, BOOL bAsUnicode, BOOL bFormat)
{
	DWORD dwLastError = 0;

	BOOL bAddRoot = pNode && pNode->pszKey && *pNode->pszKey;
	if (bAddRoot && !AddRoot(&pNode))
		bAddRoot = FALSE;

	if (bIsFile)
	{
		HANDLE hFile = CreateFile(pszBuffer, GENERIC_WRITE, FILE_SHARE_WRITE, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
		if (hFile != INVALID_HANDLE_VALUE)
		{
			SerializeToFile(hFile, pNode, bFormat ? JSON_INDENT : 0, bAsUnicode);
			CloseHandle(hFile);
		}
		else
		{
			dwLastError = GetLastError();
		}
	}
	else
	{
		int iPos = 0;
		SerializeToBuffer(&pszBuffer, &cchBuffer, pNode, &iPos, bFormat ? JSON_INDENT : 0, FALSE, &dwLastError);
	}

	if (bAddRoot)
		GlobalFree(pNode);

	SetLastError(dwLastError);
	return dwLastError == 0;
}

PTCHAR JSON_SerializeAlloc(struct JSON_NODE* pNode, BOOL bFormat, BOOL bAsPostData)
{
	DWORD dwLastError = 0;
	int cchBuffer = 2048;
	PTCHAR pszBuffer = (PTCHAR)GlobalAlloc(GPTR, cchBuffer * sizeof(TCHAR));
	
	if (pszBuffer)
	{
		BOOL bAddRoot = pNode && pNode->pszKey && *pNode->pszKey;
		if (bAddRoot && !AddRoot(&pNode))
			bAddRoot = FALSE;

		if (bAsPostData)
		{
			struct JSON_NODE* pNext = pNode->pValue;
			int iPos = 0;
			int cchValueBuffer = 2048;
			PTCHAR pszValueBuffer = (PTCHAR)GlobalAlloc(GPTR, cchValueBuffer * sizeof(TCHAR));
			if (pszValueBuffer)
			{
				while (pNext)
				{
					switch (pNext->eType)
					{
					case JNT_ARRAY:
						{
							struct JSON_NODE* pArrayNext = pNext->pValue;
							while (pArrayNext)
							{
								int iValuePos = 0;
								MyStrCpy(&pszBuffer, &cchBuffer, &iPos, pNext->pszKey, lstrlen(pNext->pszKey), TRUE, &dwLastError);
								MyStrCpy(&pszBuffer, &cchBuffer, &iPos, TEXT("[]="), 3, TRUE, &dwLastError);

								switch (pArrayNext->eType)
								{
								case JNT_NODE:
								case JNT_ARRAY:
									SerializeToBuffer(&pszValueBuffer, &cchValueBuffer, pArrayNext, &iValuePos, 0, TRUE, &dwLastError);
									break;
								default:
									MyStrCpy(&pszValueBuffer, &cchValueBuffer, &iValuePos, pArrayNext->pszValue, lstrlen(pArrayNext->pszValue), TRUE, &dwLastError);
									break;
								}

								iValuePos = EscapePostData(&pszValueBuffer, &cchValueBuffer, &dwLastError);
								if (iValuePos > 0)
									MyStrCpy(&pszBuffer, &cchBuffer, &iPos, pszValueBuffer, iValuePos, TRUE, &dwLastError);

								pArrayNext = pArrayNext->pNext;
								if (pArrayNext)
									MyStrCpy(&pszBuffer, &cchBuffer, &iPos, TEXT("&"), 1, TRUE, &dwLastError);
							}
						}
						break;

					default:
						{
							int iValuePos = 0;
							MyStrCpy(&pszBuffer, &cchBuffer, &iPos, pNext->pszKey, lstrlen(pNext->pszKey), TRUE, &dwLastError);

							if (pNext->eType != JNT_VALUE || pNext->eType == JNT_VALUE && lstrcmpi(pNext->pszValue, TEXT("true")) != 0)
							{
								MyStrCpy(&pszBuffer, &cchBuffer, &iPos, TEXT("="), 1, TRUE, &dwLastError);

								if (pNext->eType == JNT_NODE)
									SerializeToBuffer(&pszValueBuffer, &cchValueBuffer, pNext->pValue, &iValuePos, bFormat ? JSON_INDENT : 0, TRUE, &dwLastError);
								else
									MyStrCpy(&pszValueBuffer, &cchValueBuffer, &iValuePos, pNext->pszValue, lstrlen(pNext->pszValue), TRUE, &dwLastError);

								iValuePos = EscapePostData(&pszValueBuffer, &cchValueBuffer, &dwLastError);
								if (iValuePos > 0)
									MyStrCpy(&pszBuffer, &cchBuffer, &iPos, pszValueBuffer, iValuePos, TRUE, &dwLastError);
							}
						}
						break;
					}

					pNext = pNext->pNext;
					if (pNext)
						MyStrCpy(&pszBuffer, &cchBuffer, &iPos, TEXT("&"), 1, TRUE, &dwLastError);
				}

				GlobalFree(pszValueBuffer);
			}
		}
		else
		{
			int iPos = 0;
			SerializeToBuffer(&pszBuffer, &cchBuffer, pNode, &iPos, bFormat ? JSON_INDENT : 0, TRUE, &dwLastError);
		}

		if (bAddRoot)
			GlobalFree(pNode);
	}

	SetLastError(dwLastError);
	return pszBuffer;
}

PTCHAR JSON_Expand(struct JSON_NODE* pNode)
{
	PTCHAR pszExpanded = NULL;
	int i, j, cch = lstrlen(pNode->pszValue);
	TCHAR szUnicode[7];

	pszExpanded = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * (cch + 1));
	if (pszExpanded)
	{
		for (i = 0, j = 0; i < cch; i++, j++)
		{
			if (pNode->pszValue[i] == TEXT('\\'))
			{
				switch (pNode->pszValue[i + 1])
				{
				case TEXT('"'):
				case TEXT('\\'):
				case TEXT('/'):
					pszExpanded[j] = pNode->pszValue[i + 1];
					i++;
					break;
				case TEXT('b'):
					pszExpanded[j] = TEXT('\b');
					i++;
					break;
				case TEXT('f'):
					pszExpanded[j] = TEXT('\f');
					i++;
					break;
				case TEXT('n'):
					pszExpanded[j] = TEXT('\n');
					i++;
					break;
				case TEXT('r'):
					pszExpanded[j] = TEXT('\r');
					i++;
					break;
				case TEXT('t'):
					pszExpanded[j] = TEXT('\t');
					i++;
					break;
				case TEXT('u'):
					wsprintf(szUnicode, TEXT("0x%c%c%c%c"), pNode->pszValue[i + 2], pNode->pszValue[i + 3], pNode->pszValue[i + 4], pNode->pszValue[i + 5]);
#ifdef UNICODE
					pszExpanded[j] = (WCHAR)myatoi(szUnicode);
#else
					wsprintfW((PWCHAR)szUnicode, L"%c", myatoi(szUnicode));
					if (WideCharToMultiByte(CP_ACP, WC_COMPOSITECHECK, (PWCHAR)szUnicode, 1, NULL, 0, NULL, NULL) == 1)
						WideCharToMultiByte(CP_ACP, WC_COMPOSITECHECK, (PWCHAR)szUnicode, 1, pszExpanded + j, 1, NULL, NULL);
					else
						lstrcpyA(pszExpanded + j, "?");
#endif
					i += 5;
					break;
				}
			}
			else
			{
				pszExpanded[j] = pNode->pszValue[i];
			}
		}
	}

	return pszExpanded;
}

PTCHAR JSON_Escape(PTCHAR pszValue, enum JSON_ESCAPE_FLAGS eFlags)
{
	PTCHAR pszEscaped;
	int i, cch = lstrlen(pszValue), cchNew = cch;
	BOOL bAlreadyQuoted = cch > 1 && pszValue[0] == TEXT('"') && pszValue[cch - 1] == TEXT('"');
	BOOL bQuote = (eFlags & JEF_QUOTE) && ((eFlags & JEF_ALWAYS_QUOTE) || !bAlreadyQuoted);
	if (bQuote)
		cchNew += 2;
	
	for (i = 0; i < cch; i++)
	{
		switch (pszValue[i])
		{
		case TEXT('\b'):
		case TEXT('\f'):
		case TEXT('\n'):
		case TEXT('\r'):
		case TEXT('\t'):
		case TEXT('\\'):
		case TEXT('"'):
			cchNew++;
			break;
		default:
			if ((eFlags & JEF_ESCAPE_UNICODE) && (pszValue[i] < 32 || pszValue[i] > 126))
				cchNew += 6;
			break;
		}
	}

	pszEscaped = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * (cchNew + 1));

	if (pszEscaped && cch < cchNew)
	{
		int j;
		
		if (bQuote)
		{
			pszEscaped[0] = TEXT('"');
			i = 0;
			j = 1;
		}
		else if (bAlreadyQuoted)
		{
			pszEscaped[0] = TEXT('"');
			i = 1;
			j = 1;
			cch--;
		}
		else
		{
			i = 0;
			j = 0;
		}

		for (; i < cch; i++, j++)
		{
			switch (pszValue[i])
			{
			case TEXT('\b'):
				pszEscaped[j++] = TEXT('\\');
				pszEscaped[j] = TEXT('b');
				break;
			case TEXT('\f'):
				pszEscaped[j++] = TEXT('\\');
				pszEscaped[j] = TEXT('f');
				break;
			case TEXT('\n'):
				pszEscaped[j++] = TEXT('\\');
				pszEscaped[j] = TEXT('n');
				break;
			case TEXT('\r'):
				pszEscaped[j++] = TEXT('\\');
				pszEscaped[j] = TEXT('r');
				break;
			case TEXT('\t'):
				pszEscaped[j++] = TEXT('\\');
				pszEscaped[j] = TEXT('t');
				break;
			case TEXT('\\'):
			case TEXT('"'):
				pszEscaped[j++] = TEXT('\\');
				pszEscaped[j] = pszValue[i];
				break;
			default:
				if ((eFlags & JEF_ESCAPE_UNICODE) && (pszValue[i] < 32 || pszValue[i] > 126))
				{
					j += wsprintf(pszEscaped + j, TEXT("\\u%04x"), (unsigned char)pszValue[i]) - 1;
				}
				else
				{
					pszEscaped[j] = pszValue[i];
				}
				break;
			}
		}

		if (bQuote || bAlreadyQuoted)
		{
			pszEscaped[j] = TEXT('"');
		}
	}
	else
	{
		lstrcpy(pszEscaped, pszValue);
	}

	return pszEscaped;
}

PCHAR JSON_FromUnicode(PWCHAR pwszText, int* pcchText, UINT nCodePage)
{
	int cbConverted = WideCharToMultiByte(nCodePage, WC_COMPOSITECHECK, pwszText, *pcchText, NULL, 0, NULL, NULL);
	if (cbConverted > 0)
	{
		PCHAR pszBuffer = (PCHAR)GlobalAlloc(GPTR, sizeof(CHAR) * (cbConverted + 1));
		if (pszBuffer)
		{
			if (WideCharToMultiByte(nCodePage, WC_COMPOSITECHECK, pwszText, *pcchText, pszBuffer, cbConverted, NULL, NULL) > 0)
			{
				*pcchText = cbConverted;
				return pszBuffer;
			}

			GlobalFree(pszBuffer);
		}
	}

	return NULL;
}

PWCHAR JSON_ToUnicode(PCHAR pszText, int* pcbText)
{
	int cchConverted = MultiByteToWideChar(CP_ACP, MB_COMPOSITE, pszText, *pcbText, NULL, 0);
	if (cchConverted > 0)
	{
		PWCHAR pwszBuffer = (PWCHAR)GlobalAlloc(GPTR, sizeof(WCHAR) * (cchConverted + 1));
		if (pwszBuffer)
		{
			if (MultiByteToWideChar(CP_ACP, MB_COMPOSITE, pszText, *pcbText, pwszBuffer, cchConverted) > 0)
			{
				*pcbText = cchConverted;
				return pwszBuffer;
			}

			GlobalFree(pwszBuffer);
		}
	}

	return NULL;
}

static BOOL _Sort(enum JSON_SORT_FLAGS eFlags, struct JSON_NODE* pCurrent, struct JSON_NODE* pNext)
{
	BOOL bMove = FALSE;

	if (eFlags & JSF_BY_KEYS)
	{
		if (pNext->eType == JNT_NODE)
		{
			if (eFlags & JSF_RECURSIVE)
				JSON_Sort(pNext, eFlags);
		}

		if (eFlags & JSF_NUMERIC)
		{
			if (myatoi(pCurrent->pszKey) > myatoi(pNext->pszKey))
				bMove = TRUE;
		}
		else if (eFlags & JSF_CASE_SENSITIVE)
		{
			if (lstrcmp(pCurrent->pszKey, pNext->pszKey) > 0)
				bMove = TRUE;
		}
		else
		{
			if (lstrcmpi(pCurrent->pszKey, pNext->pszKey) > 0)
				bMove = TRUE;
		}
	}
	else
	{
		if (pNext->eType == JNT_ARRAY || pNext->eType == JNT_NODE)
		{
			if (eFlags & JSF_RECURSIVE)
				JSON_Sort(pNext, eFlags);
			return FALSE;
		}

		if (pCurrent->eType == JNT_ARRAY || pCurrent->eType == JNT_NODE)
		{
			return FALSE;
		}

		if (eFlags & JSF_NUMERIC)
		{
			if (myatoi(pCurrent->pszValue) > myatoi(pNext->pszValue))
				bMove = TRUE;
		}
		else if (eFlags & JSF_CASE_SENSITIVE)
		{
			if (lstrcmp(pCurrent->pszValue, pNext->pszValue) > 0)
				bMove = TRUE;
		}
		else
		{
			if (lstrcmpi(pCurrent->pszValue, pNext->pszValue) > 0)
				bMove = TRUE;
		}
	}

	if (eFlags & JSF_DESCENDING)
		bMove = !bMove;

	return bMove;
}

void JSON_Sort(struct JSON_NODE* pNode, enum JSON_SORT_FLAGS eFlags)
{
	if (pNode->eType != JNT_NODE && pNode->eType != JNT_ARRAY)
		return;

	while (TRUE)
	{
		BOOL bSwapped = FALSE;
		struct JSON_NODE** ppLink = &pNode->pValue;
		struct JSON_NODE* pCurrent = pNode->pValue;
		struct JSON_NODE* pNext;

		while (pNext = pCurrent->pNext)
		{
			if (_Sort(eFlags, pCurrent, pNext))
			{
				pCurrent->pNext = pNext->pNext;
				pNext->pNext = pCurrent;
				*ppLink = pCurrent = pNext;
				bSwapped |= TRUE;
			}

			ppLink = &pCurrent->pNext;
			pCurrent = pNext;
		}

		if (!bSwapped)
			break;
	}
}