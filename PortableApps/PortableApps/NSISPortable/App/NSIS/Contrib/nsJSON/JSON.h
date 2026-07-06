#ifndef __JSON_H__
#define __JSON_H__

#define JSON_INDENT 1
#define JSON_INDENT_CHAR "\t"

enum JSON_WORD_TYPE
{
	JWT_STRING,
	JWT_OTHER,
	JWT_NONE
};

enum JSON_NODE_TYPE
{
	// node = key: [node]
	JNT_NODE,
	// node = key: [ array ]
	JNT_ARRAY,
	// node = key: value
	JNT_VALUE,
	// node = key: "value"
	JNT_QUOTED_VALUE
};

enum JSON_SET_FLAGS
{
	JSF_NONE = 0,
	JSF_IS_FILE = 1,
	JSF_IS_UNICODE = 2,
	JSF_IS_RAW = 4
};

enum JSON_ESCAPE_FLAGS
{
	JEF_NONE = 0,
	JEF_ESCAPE_UNICODE = 1,
	JEF_QUOTE = 2,
	JEF_ALWAYS_QUOTE = 4
};

struct JSON_NODE
{
	enum JSON_NODE_TYPE eType;
	struct JSON_NODE* pNext;
	PTCHAR pszKey;
	union
	{
		PTCHAR pszValue;
		struct JSON_NODE* pValue;
	};
};

enum JSON_SORT_FLAGS
{
	// Sort descending instead of ascending.
	JSF_DESCENDING = 1,
	// Numeric comparison rather than string.
	JSF_NUMERIC = 2,
	// Sort case sensitive.
	JSF_CASE_SENSITIVE = 4,
	// Sort by keys rather than by values.
	JSF_BY_KEYS = 8,
	// Sort recursively.
	JSF_RECURSIVE = 16
};

struct JSON_NODE* JSON_Create();
BOOL JSON_IsTrue(struct JSON_NODE* pNode);
PTCHAR JSON_GetQuotedValue(struct JSON_NODE* pNode, const PTCHAR pszDefaultValue);
void JSON_Delete(struct JSON_NODE** ppNode, struct JSON_NODE* pPrev);
int JSON_Count(struct JSON_NODE* pNode);
struct JSON_NODE* JSON_Get(struct JSON_NODE* pNode, PTCHAR pszKey, BOOL bKeyIsIndex);
struct JSON_NODE* JSON_GetEx(struct JSON_NODE* pNode, PTCHAR pszKey, BOOL bKeyIsIndex, BOOL bCreate, BOOL* pbCreated);
struct JSON_NODE* JSON_Next(struct JSON_NODE** ppNode, PTCHAR pszKey, BOOL bKeyIsIndex, BOOL bCreate, BOOL* pbCreated);
BOOL JSON_Set(struct JSON_NODE* pNode, PBYTE pbValue, enum JSON_SET_FLAGS eFlags);
BOOL JSON_SetEx(struct JSON_NODE* pNode, PTCHAR pszKey, BOOL bKeyIsIndex, PBYTE pbValue, enum JSON_SET_FLAGS eFlags);
BOOL JSON_Serialize(struct JSON_NODE* pNode, PTCHAR pszBuffer, int cchBuffer, BOOL bIsFile, BOOL bAsUnicode, BOOL bFormat);
PTCHAR JSON_SerializeAlloc(struct JSON_NODE* pNode, BOOL bFormat, BOOL bAsPostData);
PTCHAR JSON_Expand(struct JSON_NODE* pNode);
PTCHAR JSON_Escape(PTCHAR pszValue, enum JSON_ESCAPE_FLAGS eFlags);
PCHAR JSON_FromUnicode(PWCHAR pwszText, int* pcchText, UINT nCodePage);
PWCHAR JSON_ToUnicode(PCHAR pszText, int* pcbText);
void JSON_Sort(struct JSON_NODE* pNode, enum JSON_SORT_FLAGS eFlags);

#endif