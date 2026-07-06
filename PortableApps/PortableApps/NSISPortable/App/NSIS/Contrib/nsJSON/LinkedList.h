#ifndef __LinkedList_H__
#define __LinkedList_H__

struct LinkedListNode
{
	PTCHAR Key;
	PVOID Value;
	struct LinkedListNode* Next;
};

struct LinkedList
{
	struct LinkedListNode* First;
};

struct LinkedList* LinkedListCreate();

typedef void (*LinkedListDestroyCallback)(struct LinkedListNode* pListNode);

void LinkedListDestroy(struct LinkedList** ppList, LinkedListDestroyCallback callback);

void LinkedListDelete(struct LinkedList** ppList, const PTCHAR szKey);

struct LinkedListNode* LinkedListGet(struct LinkedList* pList, const PTCHAR szKey, const BOOL bCreate);

#endif