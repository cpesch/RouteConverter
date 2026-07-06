#include <Windows.h>
#include "LinkedList.h"

struct LinkedList* LinkedListCreate()
{
	return (struct LinkedList*)GlobalAlloc(GPTR, sizeof(struct LinkedList));
}

void LinkedListDestroy(struct LinkedList** ppList, LinkedListDestroyCallback callback)
{
	if (ppList && *ppList)
	{
		struct LinkedListNode* pNode = (*ppList)->First;

		while (pNode)
		{
			struct LinkedListNode* pNext = pNode->Next;
			if (callback)
				callback(pNode);
			GlobalFree(pNode->Key);
			GlobalFree(pNode);
			pNode = pNext;
		}

		GlobalFree(*ppList);
		*ppList = NULL;
	}
}

void LinkedListDelete(struct LinkedList** ppList, const PTCHAR szKey)
{
	if (ppList && *ppList)
	{
		struct LinkedListNode* pNode = (*ppList)->First;
		struct LinkedListNode* pPrev = NULL;
		PTCHAR pszKey = szKey ? szKey : TEXT("");

		while (pNode)
		{
			struct LinkedListNode* pNext = pNode->Next;

			if (lstrcmpi(pNode->Key, pszKey) == 0)
			{
				if (pPrev)
					pPrev->Next = pNext;

				if ((*ppList)->First == pNode)
				{
					GlobalFree(*ppList);
					*ppList = NULL;
				}

				GlobalFree(pNode->Key);
				GlobalFree(pNode);
				break;
			}

			pPrev = pNode;
			pNode = pNext;
		}
	}
}

struct LinkedListNode* LinkedListGet(struct LinkedList* pList, const PTCHAR szKey, const BOOL bCreate)
{
	if (pList)
	{
		struct LinkedListNode* pNode = pList->First;
		PTCHAR pszKey = szKey ? szKey : TEXT("");

		while (pNode)
		{
			if (lstrcmpi(pNode->Key, pszKey) == 0)
				return pNode;
			pNode = pNode->Next;
		}

		if (bCreate)
		{
			pNode = (struct LinkedListNode*)GlobalAlloc(GPTR, sizeof(struct LinkedListNode));
			pNode->Key = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR) * (lstrlen(pszKey) + 1));
			lstrcpy(pNode->Key, pszKey);
			pNode->Next = pList->First;
			pList->First = pNode;
			return pNode;
		}
	}

	return NULL;
}