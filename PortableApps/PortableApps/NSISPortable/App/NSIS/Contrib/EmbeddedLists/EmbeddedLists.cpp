#include <windows.h>
#ifdef UNICODE
#include "nsis_unicode/pluginapi.h"
#else
#include "nsis_ansi/pluginapi.h"
#endif
#include "resource.h"
#include <commctrl.h>

/**
    EmbeddedLists v1.4 RC2 by Afrow UK
    A plugin dll for NSIS that shows list controls.
    See Docs\EmbeddedLists\Readme.txt

    Last modified: 8th July 2010
*/

#define TEXT_CHECKALL         TEXT("Check all")
#define TEXT_UNCHECKALL       TEXT("Uncheck all")
#define TEXT_SELECTALL        TEXT("Select all")
#define TEXT_UNSELECTALL      TEXT("Unselect all")

// Common keys.
#define KEY_TEXT              TEXT("Text")

// Settings.
#define SEC_SETTINGS          TEXT("Settings")
#define KEY_TYPE              TEXT("Type")              // ListView|TreeView
#define KEY_HEADINGTEXT       TEXT("HeadingText")
#define KEY_GROUPTEXT         TEXT("GroupText")
#define KEY_CAPTION           TEXT("Caption")
#define KEY_NEXTTOGGLE        TEXT("ToggleNextButton")  // 0|1
#define KEY_CHECKBOXES        TEXT("CheckBoxes")
#define KEY_SORT              TEXT("Sort")              // none|ascending|descending
#define KEY_SORTBYCOLUMNCLICK TEXT("SortByColumnClick") // 0|1
#define KEY_SORTBYCOLUMN      TEXT("SortByColumn")
#define KEY_COLHEADER         TEXT("ColumnHeader")      // 0|1
#define KEY_SINGLESELECT      TEXT("SingleSelect")      // 0|1
#define KEY_LABELEDIT         TEXT("LabelEdit")         // 0|1
#define KEY_NOITEMSELECT      TEXT("NoItemSelection")   // 0|1
#define KEY_PARENTCHECK       TEXT("ParentCheck")       // 0|1
#define KEY_RECT              TEXT("Rect")
#define KEY_VIEWCONTROLONLY   TEXT("ViewListOnly")      // 0|1
#define KEY_RETURNITEMTEXT    TEXT("ReturnItemText")    // 0|1
#define KEY_USECHECKBITMAP    TEXT("UseCheckBitmap")    // 0|1

// Icon lists.
#define SEC_ICONLIST          TEXT("Icons")
#define KEY_ICONCOUNT         TEXT("IconCount")
#define KEY_ICONFILE          TEXT("Icon")              // Icon#=

// ListView Columns.
#define SEC_COLUMNS           TEXT("Columns")
#define KEY_COLUMN            TEXT("Column")
#define KEY_WIDTH             TEXT("Width")

// Items/Nodes.
#define SEC_ITEM              TEXT("Item")
#define KEY_SUBITEM           TEXT("SubItem")           // SubItem#=
#define KEY_ICONINDEX         TEXT("IconIndex")
#define KEY_SELECTED          TEXT("Selected")          // 0|1
#define KEY_CHECKED           TEXT("Checked")           // 0|1
#define KEY_POSITION          TEXT("Position")
#define KEY_EXPANDED          TEXT("Expanded")          // 0|1
#define KEY_BOLDTEXT          TEXT("BoldText")          // 0|1
#define KEY_DISABLECHECK      TEXT("DisableCheck")      // 0|1|2
#define KEY_DISABLEEDIT       TEXT("DisableLabelEdit")  // 0|1
// Uses KEY_TEXT

#define DEFAULT_RECT 1018

// Nothing selected return value.
#define OUT_ERROR         TEXT("ERROR")
#define OUT_NEXT          TEXT("NEXT")
#define OUT_BACK          TEXT("BACK")
#define OUT_CANCEL        TEXT("CANCEL")
#define OUT_ENDSTACK      TEXT("/END")

#define DLG_LISTVIEW      TEXT("ListView")
#define DLG_TREEVIEW      TEXT("TreeView")

#define LVIS_UNCHECKED          INDEXTOSTATEIMAGEMASK(1)
#define LVIS_CHECKED            INDEXTOSTATEIMAGEMASK(2)
#define TVIS_UNCHECKED          INDEXTOSTATEIMAGEMASK(1)
#define TVIS_CHECKED            INDEXTOSTATEIMAGEMASK(2)

#define LVIS_IMAGE_BLANK        INDEXTOSTATEIMAGEMASK(0)
#define LVIS_IMAGE_UNCHECKED    INDEXTOSTATEIMAGEMASK(2)
#define LVIS_IMAGE_CHECKED      INDEXTOSTATEIMAGEMASK(3)
#define LVIS_IMAGE_PCHECKED     INDEXTOSTATEIMAGEMASK(4)
#define LVIS_IMAGE_DISUNCHECKED INDEXTOSTATEIMAGEMASK(5)
#define LVIS_IMAGE_DISCHECKED   INDEXTOSTATEIMAGEMASK(6)

#define TVIS_IMAGE_BLANK        INDEXTOSTATEIMAGEMASK(0)
#define TVIS_IMAGE_UNCHECKED    INDEXTOSTATEIMAGEMASK(1)
#define TVIS_IMAGE_CHECKED      INDEXTOSTATEIMAGEMASK(2)
#define TVIS_IMAGE_PCHECKED     INDEXTOSTATEIMAGEMASK(3)
#define TVIS_IMAGE_DISUNCHECKED INDEXTOSTATEIMAGEMASK(4)
#define TVIS_IMAGE_DISCHECKED   INDEXTOSTATEIMAGEMASK(5)

#ifndef GetWindowLongPtr
#define GetWindowLongPtr GetWindowLong
#endif

#ifndef SetWindowLongPtr
#define SetWindowLongPtr SetWindowLong
#endif

#ifndef GWLP_WNDPROC
#define GWLP_WNDPROC GWL_WNDPROC
#endif

#ifndef DWLP_DLGPROC
#define DWLP_DLGPROC DWL_DLGPROC
#endif

#ifndef LVM_SORTITEMSEX
#define LVM_SORTITEMSEX (LVM_FIRST + 81)
#endif

#ifndef ListView_SortItemsEx
#define ListView_SortItemsEx(hwndLV, _pfnCompare, _lPrm) \
  (BOOL)SendMessage((hwndLV), LVM_SORTITEMSEX, (WPARAM)(LPARAM)(_lPrm), (LPARAM)(PFNLVCOMPARE)(_pfnCompare))
#endif

#ifndef LVS_EX_LABELTIP
#define LVS_EX_LABELTIP 0x00004000
#endif

#define ReadINIStr(ReturnStr,Section,Key,DefaultVal) \
  GetPrivateProfileString(Section,Key,DefaultVal,ReturnStr,g_stringsize,g_szINIFilePath)

#define ReadINIInt(ReturnInt,Section,Key,DefaultVal) \
  (ReturnInt = GetPrivateProfileInt(Section,Key,DefaultVal,g_szINIFilePath))

#define WriteINIStr(Section,Key,Val) \
  WritePrivateProfileString(Section,Key,Val,g_szINIFilePath)

#ifndef GET_X_LPARAM
#define GET_X_LPARAM(lp) ((int)(short)LOWORD(lp))
#endif

#ifndef GET_Y_LPARAM
#define GET_Y_LPARAM(lp) ((int)(short)HIWORD(lp))
#endif

HINSTANCE g_hInstance;
HWND      g_hWndParent;
HWND      g_hDialog;
HWND      g_hCtl;

TCHAR g_szINIFilePath[MAX_PATH];

WNDPROC ParentDlgProcOld;

BOOL g_done = FALSE, g_is_cancel = FALSE, g_is_back = FALSE, g_bInitDialog = FALSE;

/* Check boxes or not?
   0 = no
   1 = yes
   2 = partialy checked supported*/
int g_iCheckBoxes = FALSE;
// Allow label editing?
BOOL g_bLabelEdit = FALSE;
BOOL g_bParentCheck = FALSE;
// Allow item selection?
BOOL g_bNoItemSelect = FALSE;
// Only display tree view / list view control on dialog.
BOOL g_bViewControlOnly = FALSE;
// Return the item text rather than the item number.
BOOL g_bReturnItemText = FALSE;
// Which dialog...
int  g_iDialog = 0;
/* 0 = list view
   1 = tree view
*/
long g_iListItemCount = 0;

// Stores info for item sorting.
struct SORTBY
{
  int iColumn;   // Column number (1 based).
  int iSort;     // 0 = ascending, 1 = descending.
};
typedef SORTBY* PSORTBY;

// Stores old item information.
struct OLDITEM
{
  int iChecked;      // 0 = unchecked, 1 = checked, 2 = partially unchecked, 3 = partially checked
  int iDisableCheck; // 0 = no disable, 2 = disabled, 3 = hidden
  BOOL bDisableEdit;
  int iOldIndex;
  PTCHAR pszOldText;
};
typedef OLDITEM* POLDITEM;

BOOL CALLBACK ListView_ParentDlgProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam);
BOOL CALLBACK TreeView_ParentDlgProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam);
#define NSISFUNC(name) extern "C" void __declspec(dllexport) name(HWND hWndParent, int string_size, PTCHAR variables, stack_t** stacktop, extra_parameters* extra)
BOOL g_bInited;
#define DLL_INIT() \
{ \
  if (!g_bInited) \
  { \
    g_hWndParent = hWndParent; \
    EXDLL_INIT(); \
    extra->RegisterPluginCallback(g_hInstance, PluginCallback); \
    g_bInited = TRUE; \
  } \
}

// Creates an OLDITEM.
POLDITEM CreateOldItem(int iSectionCount, PTCHAR pszSectionName, PTCHAR pszValue)
{
  int iValue;
  POLDITEM poi = (POLDITEM)GlobalAlloc(GPTR, sizeof(OLDITEM));
  {
    // Store original item info.
    poi->iOldIndex = iSectionCount;
    poi->pszOldText = (PTCHAR)GlobalAlloc(GPTR, sizeof(TCHAR)*(lstrlen(pszValue)+1));
    lstrcpy(poi->pszOldText, pszValue);
    
    if (g_iCheckBoxes)
    {
      // Is this item checked.
      if (ReadINIInt(iValue, pszSectionName, KEY_CHECKED, 0))
        poi->iChecked = TRUE;
      else
        poi->iChecked = FALSE;

      // Is this item check state changable?
      if (ReadINIInt(iValue, pszSectionName, KEY_DISABLECHECK, 0) != 0)
      {
        if (iValue == 2)
          poi->iDisableCheck = 2;
        else
          poi->iDisableCheck = TRUE;
      }
      else
        poi->iDisableCheck = FALSE;
    }

    // Is this item label editable?
    if (ReadINIInt(iValue, pszSectionName, KEY_DISABLEEDIT, 0) != 0)
      poi->bDisableEdit = TRUE;
  }

  return poi;
}

// Sets common control texts.
void SetCommonTexts(HWND hWndDlg)
{
  PTCHAR pszValue = (PTCHAR)LocalAlloc(LPTR, sizeof(TCHAR)*g_stringsize);

  // Get heading label text.
  ReadINIStr(pszValue, SEC_SETTINGS, KEY_HEADINGTEXT, TEXT(""));
  SetWindowText(GetDlgItem(hWndDlg, IDC_HEADINGTEXT), pszValue);

  // Get group box label text.
  ReadINIStr(pszValue, SEC_SETTINGS, KEY_GROUPTEXT, TEXT(""));
  SetWindowText(GetDlgItem(hWndDlg, IDC_GROUPBOX), pszValue);

  // Set dialog caption.
  if (ReadINIStr(pszValue, SEC_SETTINGS, KEY_CAPTION, TEXT("")))
    SetWindowText(g_hWndParent, pszValue);

  LocalFree(pszValue);
}

// Displays our dialog.
void ShowDialog()
{
  if (g_hDialog)
  {
    HWND hRect;
    RECT dialog_r;
    int mainWndWidth, mainWndHeight;
    int iRect;

    // Set dialog font to that of the parent window font.
    SendMessage(g_hDialog, WM_SETFONT, (WPARAM)SendMessage(g_hWndParent, WM_GETFONT, 0, 0), TRUE);
    
    // Get which rect to place our dialog on.
    ReadINIInt(iRect, SEC_SETTINGS, KEY_RECT, DEFAULT_RECT);
    hRect = GetDlgItem(g_hWndParent, iRect);

    // Check rect exists.
    if (!hRect)
    {
      pushstring(OUT_ERROR);
      return;
    }

    // Get the sizes of the UI.
    GetWindowRect(hRect, &dialog_r);
    MapWindowPoints(0, g_hWndParent, (LPPOINT)&dialog_r, 2);

    mainWndWidth = dialog_r.right - dialog_r.left;
    mainWndHeight = dialog_r.bottom - dialog_r.top;

    // Set our window size to fit the UI size.
    iRect = MoveWindow(
      g_hDialog,
      dialog_r.left,
      dialog_r.top,
      mainWndWidth,
      mainWndHeight,
      FALSE
    );

    // Do we only want to display the list / tree view in dialog?
    if (ReadINIInt(iRect, SEC_SETTINGS, KEY_VIEWCONTROLONLY, 0) == 1)
    {
      // Remove unwanted controls.
      DestroyWindow(GetDlgItem(g_hDialog, IDC_HEADINGTEXT));
      DestroyWindow(GetDlgItem(g_hDialog, IDC_GROUPBOX));
      
      // Set our window size to fit the UI size.
      MoveWindow(
        g_hCtl,
        0,
        0,
        mainWndWidth,
        mainWndHeight,
        FALSE
      );
    }
    else
    {
      HWND hCtl;
      int iTop;

      // Get the size of the heading label.
      hCtl = GetDlgItem(g_hDialog, IDC_HEADINGTEXT);
      GetWindowRect(hCtl, &dialog_r);
      iTop = dialog_r.bottom - dialog_r.top;

      // Resize heading label.
      MoveWindow(
        hCtl,
        0,
        0,
        mainWndWidth,
        iTop,
        FALSE
      );

      // Get the size of the group box.
      hCtl = GetDlgItem(g_hDialog, IDC_GROUPBOX);

      // Resize group box.
      MoveWindow(
        hCtl,
        0,
        iTop,
        mainWndWidth,
        mainWndHeight - iTop - 5,
        FALSE
      );

      iTop += 15;

      // Resize list view / tree view.
      MoveWindow(
        g_hCtl,
        8,
        iTop,
        mainWndWidth - 15,
        mainWndHeight - iTop - 15,
        FALSE
      );

    }

    // Sub-class parent window procedure.
    if (g_iDialog == 0)
      // For list view dialog.
      ParentDlgProcOld = (WNDPROC)SetWindowLongPtr(g_hWndParent, DWLP_DLGPROC, (LONG)ListView_ParentDlgProc);
    else
      // For tree view dialog.
      ParentDlgProcOld = (WNDPROC)SetWindowLongPtr(g_hWndParent, DWLP_DLGPROC, (LONG)TreeView_ParentDlgProc);

    // Sets the font of IO window to be the same as the main window.
    SendMessage(g_hDialog, WM_SETFONT, (WPARAM)SendMessage(g_hWndParent, WM_GETFONT, 0, 0), (LPARAM)TRUE);

    // Tell NSIS to remove old inner dialog and pass handle of the new inner dialog.
    SendMessage(g_hWndParent, WM_NOTIFY_CUSTOM_READY, (WPARAM)g_hDialog, 0);
    ShowWindow(g_hDialog, SW_SHOW);

    g_done = FALSE;

    // Loop until the user clicks on a button.
    while (!g_done) {
      MSG msg;
      int nResult = GetMessage(&msg, NULL, 0, 0);
      if (!IsDialogMessage(g_hDialog, &msg) && !IsDialogMessage(g_hWndParent, &msg))
      {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
      }
    }

    // Set window dialog procedure back to NSIS's.
    SetWindowLongPtr(g_hWndParent, DWL_DLGPROC, (long)ParentDlgProcOld);
    DestroyWindow(g_hDialog);

    // Return page button result.
    pushstring(g_is_cancel ? OUT_CANCEL : g_is_back ? OUT_BACK : OUT_NEXT);

  }
  else
    pushstring(OUT_ERROR);
}

// Get icons from icon list in INI file.
HIMAGELIST GetIcons()
{
  static TCHAR szSectionName[32], szIconPath[MAX_PATH];
  int iIcons, iSectionCount;
  HICON hIcon;
  HIMAGELIST himlIcons = NULL;
  wsprintf(szSectionName, TEXT("%s%i"), KEY_ICONFILE, iSectionCount = 1);
  while (ReadINIStr(szIconPath, SEC_ICONLIST, szSectionName, TEXT("")))
  {
    if (!himlIcons)
      himlIcons = ImageList_Create(16, 16, ILC_MASK | ILC_COLOR32, ReadINIInt(iIcons, SEC_ICONLIST, KEY_ICONCOUNT, 8), 0);

    hIcon = (HICON)LoadImage(g_hInstance, szIconPath, IMAGE_ICON, 16, 16, LR_LOADFROMFILE | LR_LOADTRANSPARENT | LR_SHARED);
    if (hIcon)
      ImageList_AddIcon(himlIcons, hIcon);
    DestroyIcon(hIcon);

    wsprintf(szSectionName, TEXT("%s%i"), KEY_ICONFILE, ++iSectionCount);
  }
  return himlIcons;
}

// Get check state image from installer exe header.
HIMAGELIST GetStateImage(PTCHAR pszPath)
{
  HBITMAP hStateImage;
  HIMAGELIST himlState;

  if (lstrcmp(pszPath, TEXT("1")) == 0)
    hStateImage = (HBITMAP)LoadImage(GetModuleHandle(NULL), MAKEINTRESOURCE(IDB_BITMAP), IMAGE_BITMAP, 96, 16, LR_SHARED);
  else
    hStateImage = (HBITMAP)LoadImage(g_hInstance, pszPath, IMAGE_BITMAP, 96, 16, LR_LOADFROMFILE | LR_SHARED);

  if (!hStateImage)
    return NULL;

  himlState = ImageList_Create(16, 16, ILC_MASK | ILC_COLOR32, 6, 0);
  ImageList_AddMasked(himlState, hStateImage, RGB(255, 0, 255));

  DeleteObject(hStateImage);

  g_iCheckBoxes = 2;
  return himlState;
}

// Frees memory allocated for each list view item.
void ListView_FreeParams()
{
  static LVITEM lvi;
  static POLDITEM poi;
  lvi.mask = TVIF_PARAM;
  lvi.iItem = 0;
  // Get lParam.
  while (ListView_GetItem(g_hCtl, &lvi))
  {
    // Get OLDITEM data.
    poi = (POLDITEM)lvi.lParam;

    // Free memory allocated.
    GlobalFree(poi->pszOldText);
    GlobalFree(poi);

    // Next item.
    lvi.iItem += 1;
  }
}

// Get number of checked items.
BOOL ListView_IsItemChecked()
{
  LVITEM lvi;
  for (int i=0; i<ListView_GetItemCount(g_hCtl); i++)
  {
    lvi.mask = LVIF_PARAM;
    lvi.iItem = i;
    ListView_GetItem(g_hCtl, &lvi);

    if (((POLDITEM)lvi.lParam)->iChecked == TRUE)
      return TRUE;
  }
  return FALSE;
}

// Compare items in list view.
static int CALLBACK ListView_CompareFunc(LPARAM lParam1, LPARAM lParam2, LPARAM lParamSort)
{
  static PSORTBY psb;
  static LVITEM lvi;
  PTCHAR pszText1 = (PTCHAR)LocalAlloc(LPTR, sizeof(TCHAR)*g_stringsize);
  PTCHAR pszText2 = (PTCHAR)LocalAlloc(LPTR, sizeof(TCHAR)*g_stringsize);
  
  psb = (PSORTBY)lParamSort;

  lvi.mask = LVIF_TEXT;
  lvi.iSubItem = psb->iColumn-1;

  lvi.iItem = lParam1;
  lvi.pszText = pszText1;
  lvi.cchTextMax = g_stringsize;
  ListView_GetItem(g_hCtl, &lvi);

  lvi.iItem = lParam2;
  lvi.pszText = pszText2;
  lvi.cchTextMax = g_stringsize;
  ListView_GetItem(g_hCtl, &lvi);
  
  int result;
  if (psb->iSort == 0)
    result = lstrcmpi(pszText1, pszText2);
  else
    result = lstrcmpi(pszText2, pszText1);

  LocalFree(pszText1);
  LocalFree(pszText2);

  return result;
}

// Handles the parent dialog for list view dialog.
static BOOL CALLBACK ListView_ParentDlgProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
  static LVITEM lvi;
  static POLDITEM poi;
	static TCHAR szSectionName[32],
               szNumber[4];
  static BOOL bRes = FALSE;
  static int i;

  PTCHAR pszValue = (PTCHAR)LocalAlloc(LPTR, sizeof(TCHAR)*g_stringsize);

  if (uMsg == WM_NOTIFY_OUTER_NEXT && wParam == 1)
  {
    pushstring(OUT_ENDSTACK);
    lvi.mask = LVIF_TEXT | LVIF_PARAM | LVIF_STATE;
    lvi.iSubItem = 0;
    lvi.pszText = pszValue;
    lvi.cchTextMax = g_stringsize;

    // No check boxes.
    if (!g_iCheckBoxes)
    {
      for (i=0; i<g_iListItemCount; i++)
      {
        // Get item.
        lvi.stateMask = LVIS_SELECTED;
        lvi.iItem = i;
        ListView_GetItem(g_hCtl, &lvi);

        // Get old item info.
        poi = (POLDITEM)lvi.lParam;

        // Get INI file section.
        wsprintf(szSectionName, TEXT("%s %i"), SEC_ITEM, poi->iOldIndex);

        // Is item selected?
        if (lvi.state & LVIS_SELECTED)
        {
          // Add item text to stack.
          if (g_bReturnItemText)
            pushstring(lvi.pszText);
          // Add item number to stack.
          else
          {
            wsprintf(szNumber, TEXT("%i"), poi->iOldIndex);
            pushstring(szNumber);
          }
          // Save selected state in INI file.
          WriteINIStr(szSectionName, KEY_SELECTED, TEXT("1"));
        }
        else
          // Save selected state in INI file.
          WriteINIStr(szSectionName, KEY_SELECTED, TEXT("0"));

        // Save list item text if it has been modified.
        if (g_bLabelEdit)
          if (!poi->bDisableEdit)
            if (lstrcmp(poi->pszOldText, lvi.pszText) != 0)
              WriteINIStr(szSectionName, KEY_TEXT, lvi.pszText);
      }
    }
    // With check boxes.
    else
    {
      for (i=0; i<g_iListItemCount; i++)
      {
        // Get item.
        lvi.iItem = i;
        ListView_GetItem(g_hCtl, &lvi);

        // Get old item info.
        poi = (POLDITEM)lvi.lParam;

        // Get INI file section.
        wsprintf(szSectionName, TEXT("%s %i"), SEC_ITEM, poi->iOldIndex);
        // Is item checked?
        if (poi->iChecked)
        {
          // Add item text to stack.
          if (g_bReturnItemText)
            pushstring(lvi.pszText);
          // Add item number to stack.
          else
          {
            wsprintf(szNumber, TEXT("%i"), poi->iOldIndex);
            pushstring(szNumber);
          }
          // Save checked state in INI file, unless its
          // state cannot be changed.
          if (!poi->iDisableCheck)
            WriteINIStr(szSectionName, KEY_CHECKED, TEXT("1"));
        }
        else
          // Save checked state in INI file, unless its
          // state cannot be changed.
          if (!poi->iDisableCheck)
            WriteINIStr(szSectionName, KEY_CHECKED, TEXT("0"));

        // Save list item text if it has been modified.
        if (g_bLabelEdit)
          if (!poi->bDisableEdit)
            if (lstrcmp(poi->pszOldText, lvi.pszText) != 0)
              WriteINIStr(szSectionName, KEY_TEXT, lvi.pszText);
      }
    }
  }

  LocalFree(pszValue);

  bRes = CallWindowProc(ParentDlgProcOld, hWnd, uMsg, wParam, lParam);
  if (uMsg == WM_NOTIFY_OUTER_NEXT && !bRes)
  {
    if (wParam == -1)
      g_is_back = TRUE;
    else if (wParam == NOTIFY_BYE_BYE)
      g_is_cancel = TRUE;
    g_done = TRUE;
    PostMessage(g_hDialog, WM_CLOSE, 0, 0);
  }
  return bRes;
}

// Handles ListView dialog.
static LRESULT CALLBACK ListView_DlgProc(HWND hWndDlg, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
  static HWND hNext = GetDlgItem(g_hWndParent, IDC_NEXT);
  static BOOL bToggleNext = TRUE,
              bItemSelected = FALSE,
              bSingleSelect = FALSE,
              bDialogShown = FALSE;
	static TCHAR szSectionName[32],
               szKeyName[32];
  static int iValue,
             iSectionCount,
             iKeyCount;
  static HIMAGELIST himlIcons,
                    himlState;
  static LVITEM lvi;
  static LVCOLUMN lvcNew;
  static DWORD lStyle = 0;
  static SORTBY sb;

  PTCHAR pszValue = (PTCHAR)LocalAlloc(LPTR, sizeof(TCHAR)*g_stringsize);
  BOOL bResult = FALSE;

	switch (uMsg)
	{
  case WM_SHOWWINDOW:
    {
      if (g_iCheckBoxes && !bDialogShown)
      {
        // Check all items to be checked (iChecked == TRUE).
        for (int i=0; i<ListView_GetItemCount(g_hCtl); i++)
        {
          POLDITEM poi;
          lvi.mask = LVIF_PARAM;
          lvi.iItem = i;
          ListView_GetItem(g_hCtl, &lvi);
          poi = (POLDITEM)lvi.lParam;
          if (poi->iDisableCheck && (g_iCheckBoxes == 2))
          {
            if (poi->iDisableCheck == 2)
              lvi.state = LVIS_IMAGE_BLANK;
            else
            {
              if (poi->iChecked)
                lvi.state = LVIS_IMAGE_DISCHECKED;
              else
                lvi.state = LVIS_IMAGE_DISUNCHECKED;
            }
          }
          else
          {
            if (poi->iChecked)
              lvi.state = (g_iCheckBoxes == 2 ? LVIS_IMAGE_CHECKED   : LVIS_CHECKED);
            else
              lvi.state = (g_iCheckBoxes == 2 ? LVIS_IMAGE_UNCHECKED : LVIS_UNCHECKED);
          }
          lvi.mask |= LVIF_STATE;
          lvi.stateMask = LVIS_STATEIMAGEMASK;
          ListView_SetItem(g_hCtl, &lvi);
        }
      }

      // Disable next button.
      if (bToggleNext)
        if (!ListView_IsItemChecked())
          EnableWindow(hNext, FALSE);

      bDialogShown = TRUE;
    }
  break;
	case WM_INITDIALOG:

    g_hCtl = GetDlgItem(hWndDlg, IDC_LIST);
    lStyle = GetWindowLongPtr(g_hCtl, GWL_STYLE);
    lvcNew.iSubItem = 0;
    sb.iColumn = 1;
    sb.iSort = -1;
    {
      // Full row select always.
      ListView_SetExtendedListViewStyle(g_hCtl, LVS_EX_FULLROWSELECT | LVS_EX_LABELTIP);

      // Disable the next button and re-enable when a item is selected or checked?
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_NEXTTOGGLE, 0) == 0)
        bToggleNext = FALSE;

      // Do not allow item selection.
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_NOITEMSELECT, 0) == 1)
        g_bNoItemSelect = TRUE;

      // Return selected/checked items by item text rather than item number.
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_RETURNITEMTEXT, 0) == 1)
        g_bReturnItemText = TRUE;

      // Allow column clicking. If sorting is enabled, will sort list by column.
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_SORTBYCOLUMNCLICK, 0) == 0)
        lStyle |= LVS_NOSORTHEADER;

      // Add icon files to image list.
      if (himlIcons = GetIcons())
        ListView_SetImageList(g_hCtl, himlIcons, LVSIL_SMALL);

      // Add check boxes to the list view.
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_CHECKBOXES, 0))
      {
        g_iCheckBoxes = TRUE;

        // Use NSIS check bitmap for state image list.
        if (ReadINIStr(pszValue, SEC_SETTINGS, KEY_USECHECKBITMAP, TEXT("1")) && lstrcmp(pszValue, TEXT("0")) != 0)
        {
          if (himlState = GetStateImage(pszValue))
            ListView_SetImageList(g_hCtl, himlState, LVSIL_STATE);
          else
            ListView_SetExtendedListViewStyle(g_hCtl, LVS_EX_FULLROWSELECT | LVS_EX_CHECKBOXES | LVS_EX_LABELTIP);
        }
        else
          ListView_SetExtendedListViewStyle(g_hCtl, LVS_EX_FULLROWSELECT | LVS_EX_CHECKBOXES | LVS_EX_LABELTIP);
      }
      
      // Hide column headers.
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_COLHEADER, 0) == 0)
      {
        RECT r;
        GetClientRect(g_hCtl, &r);
        lStyle |= LVS_NOCOLUMNHEADER;
        lvcNew.mask = LVCF_TEXT | LVCF_WIDTH;
        lvcNew.pszText = TEXT("");
        lvcNew.cx = r.right-6;
        ListView_InsertColumn(g_hCtl, lvcNew.iSubItem, &lvcNew);
      }

      // List view is single select.
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_SINGLESELECT, 0))
      {
        bSingleSelect = TRUE;
        lStyle |= LVS_SINGLESEL;
      }
      // Items are editable.
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_LABELEDIT, 0))
      {
        g_bLabelEdit = TRUE;
        lStyle |= LVS_EDITLABELS;
      }

      // Sort list view items alphabetically in ascending order.
      if (ReadINIStr(pszValue, SEC_SETTINGS, KEY_SORT, TEXT("none")))
      {
        if (lstrcmpi(pszValue, TEXT("ascending")) == 0)
        {
          sb.iSort = 0;
          ReadINIInt(sb.iColumn, SEC_SETTINGS, KEY_SORTBYCOLUMN, 1);
        }
        else if (lstrcmpi(pszValue, TEXT("descending")) == 0)
        {
          sb.iSort = 1;
          ReadINIInt(sb.iColumn, SEC_SETTINGS, KEY_SORTBYCOLUMN, 1);
        }
      }

      // Add column headings to list view.
      if ((lStyle & LVS_NOCOLUMNHEADER) != LVS_NOCOLUMNHEADER)
      {
        wsprintf(szKeyName, TEXT("%s%i"), KEY_COLUMN, iKeyCount = 1);
        while (ReadINIStr(pszValue, SEC_COLUMNS, szKeyName, TEXT("")))
        {
          // Set column text.
          lvcNew.mask = LVCF_TEXT | LVCF_WIDTH;
          lvcNew.pszText = pszValue;
          lvcNew.cchTextMax = g_stringsize;

          // Set column width.
          wsprintf(szKeyName, TEXT("%s%s"), szKeyName, KEY_WIDTH);
          ReadINIInt(iValue, SEC_COLUMNS, szKeyName, 100);
          lvcNew.cx = iValue;

          // Add column.
          ListView_InsertColumn(g_hCtl, ++lvcNew.iSubItem, &lvcNew);

          // Next column.
          wsprintf(szKeyName, TEXT("%s%i"), KEY_COLUMN, ++iKeyCount);
        }
      }

      // Add list items.
      g_iListItemCount = 0;
      wsprintf(szSectionName, TEXT("%s %i"), SEC_ITEM, iSectionCount = 1);
      while (ReadINIStr(pszValue, szSectionName, KEY_TEXT, TEXT("")))
      {
        // Set general item properties.
        lvi.iSubItem = 0;
        lvi.mask = LVIF_TEXT | LVIF_PARAM;
        lvi.pszText = pszValue;
        lvi.cchTextMax = g_stringsize;
        lvi.iItem = g_iListItemCount;

        // Store original item info in item tag.
        lvi.lParam = (LPARAM)CreateOldItem(iSectionCount, szSectionName, pszValue);

        // Increment item count.
        g_iListItemCount++;

        // Have an image list present?
        if (himlIcons)
        {
          // Setting an icon on the item?
          if (ReadINIInt(iValue, szSectionName, KEY_ICONINDEX, 0) != 0)
          {
            lvi.mask |= LVIF_IMAGE;
            lvi.iImage = iValue-1; // Conversion to zero-based.
          }
        }

        // Select item if required.
        if (ReadINIInt(iValue, szSectionName, KEY_SELECTED, 0))
        {
          if ((lStyle & LVS_SINGLESEL) == LVS_SINGLESEL)
          {
            // If we're using single selection, only
            // select one item.
            if (!bItemSelected)
            {
              lvi.mask |= LVIF_STATE;
              lvi.stateMask |= LVIS_SELECTED;
              lvi.state |= LVIS_SELECTED;
              bItemSelected = TRUE;
            }
          }
          // Using multiple selection.
          else
          {
            lvi.mask |= LVIF_STATE;
            lvi.stateMask |= LVIS_SELECTED;
            lvi.state |= LVIS_SELECTED;
          }
        }

        // Add list view item.
        ListView_InsertItem(g_hCtl, &lvi);

        // Set sub item texts.
        wsprintf(szKeyName, TEXT("%s%i"), KEY_SUBITEM, iKeyCount = 1);
        while (ReadINIStr(pszValue, szSectionName, szKeyName, TEXT("")))
        {
          // Set sub item text.
          lvi.iSubItem++;
          ListView_SetItemText(g_hCtl, lvi.iItem, lvi.iSubItem, pszValue);
          // Next sub item.
          wsprintf(szKeyName, TEXT("%s%i"), KEY_SUBITEM, ++iKeyCount);
        }

        // Next item.
        wsprintf(szSectionName, TEXT("%s %i"), SEC_ITEM, ++iSectionCount);
      }

		}
    SetCommonTexts(hWndDlg);
    SetWindowLongPtr(g_hCtl, GWL_STYLE, lStyle);

    // Sort list view items.
    if (sb.iSort != -1)
      ListView_SortItemsEx(g_hCtl, ListView_CompareFunc, &sb);

    // Set sub item to 1 now we're done using it.
    lvi.iSubItem = 0;

    // Focus list view control.
    SetFocus(g_hCtl);

	break;
  case WM_CONTEXTMENU:
    {
      // Display a popup menu with check/uncheck all or select/unselect all options.
      if ((wParam == (WPARAM)g_hCtl))
      {
        // Only display the menu if check boxes are enabled,
        // multiple selection is supported,
        // there is 1 or more items in the list.
        if ((g_iCheckBoxes || !bSingleSelect) && (iValue = ListView_GetItemCount(g_hCtl)))
        {
          HMENU hMenu = CreatePopupMenu();
          POINT pt;
          int iSelection;
          if (g_iCheckBoxes)
          {
            AppendMenu(hMenu, MF_STRING, 1, TEXT_CHECKALL);
            AppendMenu(hMenu, MF_STRING, 2, TEXT_UNCHECKALL);
          }
          if (!bSingleSelect && !g_bNoItemSelect)
          {
            AppendMenu(hMenu, MF_STRING, 3, TEXT_SELECTALL);
            AppendMenu(hMenu, MF_STRING, 4, TEXT_UNSELECTALL);
          }
          // Get the position to display the popup menu.
          if (lParam == ((UINT)-1))
          {
            RECT r;
            GetWindowRect(g_hCtl, &r);
            pt.x = r.left;
            pt.y = r.top;
          }
          else
          {
            pt.x = GET_X_LPARAM(lParam);
            pt.y = GET_Y_LPARAM(lParam);
          }
          // Display the menu and detect which item was selected.
          iSelection = TrackPopupMenu(hMenu, TPM_NONOTIFY|TPM_RETURNCMD, pt.x, pt.y, 0, g_hCtl, 0);
          if (iSelection)
          {
            for (int i=0; i<iValue; i++)
            {
              lvi.iItem = i;
              
              // Check or uncheck items.
              if ((iSelection == 1) || (iSelection == 2))
              {
                POLDITEM poi;
                lvi.mask = LVIF_PARAM;
                ListView_GetItem(g_hCtl, &lvi);
                poi = (POLDITEM)lvi.lParam;

                if (!poi->iDisableCheck)
                {
                  if (iSelection == 1)
                  {
                    if (g_iCheckBoxes == 2)
                      lvi.state = LVIS_IMAGE_CHECKED;
                    else
                      lvi.state = LVIS_CHECKED;
                    poi->iChecked = TRUE;
                  }
                  else
                  {
                    if (g_iCheckBoxes == 2)
                      lvi.state = LVIS_IMAGE_UNCHECKED;
                    else
                      lvi.state = LVIS_UNCHECKED;
                    poi->iChecked = FALSE;
                  }

                  lvi.mask |= LVIF_STATE;
                  lvi.stateMask = LVIS_STATEIMAGEMASK;
                  ListView_SetItem(g_hCtl, &lvi);
                }
              }
              else
              {
                lvi.mask = LVIF_STATE;
                lvi.stateMask = LVIS_SELECTED;
                if (iSelection == 3)
                  lvi.state = LVIS_SELECTED;
                else
                  lvi.state = 0;
                ListView_SetItem(g_hCtl, &lvi);
              }
            }
          }
        }
      }
    }
  break;
  case WM_NOTIFY:
    {
      NMHDR* pnmh = (NMHDR*)lParam;
      switch (pnmh->code)
      {
      case LVN_BEGINLABELEDIT:
        {
          NMLVDISPINFO* pdi = (NMLVDISPINFO*)lParam;
          POLDITEM poi;

          // Get OLDITEM info.
          pdi->item.mask = LVIF_PARAM;
          ListView_GetItem(g_hCtl, &pdi->item);
          poi = (POLDITEM)pdi->item.lParam;

          // Find out if the item label can be edited.
          if (poi->bDisableEdit)
          {
            // No, so press enter to cancel edit.
            keybd_event(VK_RETURN, 0, 0, 0);
            keybd_event(VK_RETURN, 0, KEYEVENTF_KEYUP, 0);
          }
          bResult = TRUE;
        }
      break;
      case LVN_ENDLABELEDIT:
        {
          NMLVDISPINFO* pdi = (NMLVDISPINFO*)lParam;
          // Label is being edited.
          if (pdi->item.pszText != NULL)
            ListView_SetItemText(g_hCtl, pdi->item.iItem, 0, pdi->item.pszText);
          bResult = TRUE;
        }
      break;
      case LVN_COLUMNCLICK:
        {
          NMLISTVIEW* pnmv = (NMLISTVIEW*)lParam;
          // Sort list view by selected column.
          if (sb.iSort == 0)
          {
            // Clicking the same column twice?
            // Will reverse sort order.
            if (pnmv->iSubItem == sb.iColumn-1)
              sb.iSort = 1;
            else
              sb.iColumn = pnmv->iSubItem+1;
            // Sort them!
            ListView_SortItemsEx(g_hCtl, ListView_CompareFunc, &sb);
          }
          else if (sb.iSort == 1)
          {
            // Clicking the same column twice?
            // Will reverse sort order.
            if (pnmv->iSubItem == sb.iColumn-1)
              sb.iSort = 0;
            else
              sb.iColumn = pnmv->iSubItem+1;
            // Sort them!
            ListView_SortItemsEx(g_hCtl, ListView_CompareFunc, &sb);
          }
        }
      break;
      case LVN_ITEMCHANGED:
        {
          NMLISTVIEW* pnmv = (NMLISTVIEW*)lParam;
          // There is a bug where double clicking on the left or right margin of an item
          // will change the state image incorrectly. This corrects the effects of the bug.
          if (g_iCheckBoxes)
          {
            POLDITEM poi = (POLDITEM)pnmv->lParam;
            if (g_iCheckBoxes == 2)
            {
              if (poi->iDisableCheck)
              {
                if (poi->iDisableCheck == 2)
                  lvi.state = LVIS_IMAGE_BLANK;
                else
                {
                  if (poi->iChecked)
                    lvi.state = LVIS_IMAGE_DISCHECKED;
                  else
                    lvi.state = LVIS_IMAGE_DISUNCHECKED;
                }
              }
              else
              {
                if (poi->iChecked)
                  lvi.state = LVIS_IMAGE_CHECKED;
                else
                  lvi.state = LVIS_IMAGE_UNCHECKED;
              }
            }
            else
            {
              if (poi->iChecked)
                lvi.state = LVIS_CHECKED;
              else
                lvi.state = LVIS_UNCHECKED;
            }
            lvi.mask = LVIF_STATE;
            lvi.stateMask = LVIS_STATEIMAGEMASK;
            ListView_SetItem(g_hCtl, &lvi);
          }
          // Prevent selection of an item.
          if (g_bNoItemSelect)
          {
            if (pnmv->iItem != -1)
              if (pnmv->uNewState & LVIS_SELECTED)
              {
                lvi.mask = LVIF_STATE;
                lvi.iItem = pnmv->iItem;
                lvi.stateMask = LVIS_SELECTED;
                lvi.state = 0;
                ListView_SetItem(g_hCtl, &lvi);
              }
          }
          else if (bToggleNext && !g_iCheckBoxes)
          {
            // Enable Next if there are one or more
            // selected items.
            if (ListView_GetSelectedCount(g_hCtl))
              EnableWindow(hNext, TRUE);
            // Otherwise, disable Next button.
            else
              EnableWindow(hNext, FALSE);
          }
        }
      break;
      // List view was clicked on.
      case NM_DBLCLK:
      case NM_CLICK:
        {
          // This contains the sub item index if it was clicked on.
          // We can ignore those clicks on sub items because they have no
          // effect on the check boxes.
          NMITEMACTIVATE *nmlvi = (NMITEMACTIVATE*)lParam;
          // The point at which the click took place
          // so we can perform a 'hit test'.
          POINT p;
          LVHITTESTINFO lvhti;

          // Do not continue if check boxes are not enabled.
          if (!g_iCheckBoxes)
            break;

          // Get mouse cursor position in tree view.
          GetCursorPos(&p);
          ScreenToClient(g_hCtl, &p);
          lvhti.pt.x = p.x;
          lvhti.pt.y = p.y;

          // Get check box user just clicked on.
          if ((lvi.iItem = ListView_HitTest(g_hCtl, &lvhti)) != -1)
          {
            POLDITEM poi;

            // Get item.
            lvi.mask = LVIF_PARAM | LVIF_STATE;
            ListView_GetItem(g_hCtl, &lvi);

            // Get OLDITEM info.
            poi = (POLDITEM)lvi.lParam;

            lvi.mask = LVIF_STATE;
            lvi.stateMask = LVIS_STATEIMAGEMASK;

            // Clicked on a check box?
            if ((lvhti.flags & LVHT_ONITEMSTATEICON) && !nmlvi->iSubItem)
            {
              // Don't do anything if we are using the check bitmap
              // and the this check box is disabled.
              if (!poi->iDisableCheck && (g_iCheckBoxes == 2))
              {
                // Set check state.
                if (poi->iChecked == TRUE)
                {
                  lvi.state = LVIS_IMAGE_UNCHECKED;
                  poi->iChecked = FALSE;
                }
                else
                {
                  lvi.state = LVIS_IMAGE_CHECKED;
                  poi->iChecked = TRUE;
                }
                ListView_SetItem(g_hCtl, &lvi);

                // Disable next button.
                if (bToggleNext)
                {
                  if (ListView_IsItemChecked())
                    EnableWindow(hNext, TRUE);
                  else
                    EnableWindow(hNext, FALSE);
                }
              }
              // If we aren't using the check bitamp...
              else if (g_iCheckBoxes == 1)
              {
                // The node check state cannot be changed...
                if (poi->iDisableCheck)
                {
                  // Check box must stay checked.
                  if (poi->iChecked)
                    lvi.state = LVIS_UNCHECKED;
                  // Check box must stay unchecked.
                  else
                    lvi.state = LVIS_CHECKED;
                }
                else
                {
                  // Check box is to be unchecked.
                  if (poi->iChecked)
                    poi->iChecked = FALSE;
                  // Check box is to be checked.
                  else
                    poi->iChecked = TRUE;

                  lvi.mask = LVIF_PARAM;
                }
                ListView_SetItem(g_hCtl, &lvi);
              }
            }
          }
        }
      break;
      }
    }
  break;
  case WM_DESTROY:
    {
      ListView_FreeParams();
    }
  break;
  case WM_CTLCOLORSTATIC:
  case WM_CTLCOLOREDIT:
  case WM_CTLCOLORDLG:
  case WM_CTLCOLORBTN:
  case WM_CTLCOLORLISTBOX:
    // Let the NSIS window handle colours, it knows best.
    bResult = SendMessage(g_hWndParent, uMsg, wParam, lParam);
  }

  LocalFree(pszValue);
	return bResult;
}

// Search for all checked nodes and push them onto NSIS stack.
void TreeView_SaveState(HTREEITEM htvi)
{
  static HTREEITEM htvic, htvicc;
  static TVITEM tvi;
  static POLDITEM poi;
	static TCHAR szSectionName[32],
               szNumber[4];

  PTCHAR pszValue = (PTCHAR)LocalAlloc(LPTR, sizeof(TCHAR)*g_stringsize);

  do
  {
    // Get node.
    tvi.mask = TVIF_HANDLE | TVIF_TEXT | TVIF_PARAM | TVIF_STATE;
    tvi.stateMask = TVIS_STATEIMAGEMASK;
    tvi.hItem = htvi;
    tvi.pszText = pszValue;
    tvi.cchTextMax = g_stringsize;
    TreeView_GetItem(g_hCtl, &tvi);

    // Get old node info.
    poi = (POLDITEM)tvi.lParam;

    // Get INI file section.
    wsprintf(szSectionName, TEXT("%s %i"), SEC_ITEM, poi->iOldIndex);

    // Can check box state be changed?
    // If not, then no state changes will have occured.
    if (!poi->iDisableCheck)
    {
      // Is item checked?
      if (poi->iChecked)
      {
        // Push checked item number or text onto stack.
        if (!g_bNoItemSelect)
        {
          // Add item text to stack.
          if (g_bReturnItemText)
            pushstring(tvi.pszText);
          // Add item number to stack.
          else
          {
            wsprintf(szNumber, TEXT("%i"), poi->iOldIndex);
            pushstring(szNumber);
          }
        }
        // Save checked state in INI file.
        WriteINIStr(szSectionName, KEY_CHECKED, TEXT("1"));
      }
      else
        // Save checked state in INI file.
        WriteINIStr(szSectionName, KEY_CHECKED, TEXT("0"));
    }

    // Save list item text if it has been modified.
    if (g_bLabelEdit)
      if (!poi->bDisableEdit)
        if (lstrcmp(poi->pszOldText, tvi.pszText) != 0)
          WriteINIStr(szSectionName, KEY_TEXT, tvi.pszText);

    // Next child node.
    if (htvic = TreeView_GetChild(g_hCtl, htvi))
    {
      // Check if this node is expanded.
      if (tvi.state & TVIS_EXPANDED)
        WriteINIStr(szSectionName, KEY_EXPANDED, TEXT("1"));
      else
        WriteINIStr(szSectionName, KEY_EXPANDED, TEXT("0"));
      TreeView_SaveState(htvic);
    }
  }
  while (htvi = TreeView_GetNextSibling(g_hCtl, htvi));

  LocalFree(pszValue);
}

// Recursively check all child nodes.
void TreeView_CheckChildNodes(HTREEITEM htvi, BOOL state)
{
  HTREEITEM htvic;
  TVITEM tvi;
  POLDITEM poi;
  do
  {
    // Get node.
    tvi.mask = TVIF_HANDLE | TVIF_PARAM;
    tvi.hItem = htvi;
    TreeView_GetItem(g_hCtl, &tvi);
    poi = (POLDITEM)tvi.lParam;

    // Check this node state is allowed to be changed...
    if (!poi->iDisableCheck)
    {
      poi->iChecked = state;
      if (state)
        tvi.state = (g_iCheckBoxes == 2 ? TVIS_IMAGE_CHECKED   : TVIS_CHECKED);
      else
        tvi.state = (g_iCheckBoxes == 2 ? TVIS_IMAGE_UNCHECKED : TVIS_UNCHECKED);
      tvi.mask |= TVIF_STATE;
      tvi.stateMask = TVIS_STATEIMAGEMASK;
      TreeView_SetItem(g_hCtl, &tvi);
    }

    // Next child node.
    if (htvic = TreeView_GetChild(g_hCtl, htvi))
      TreeView_CheckChildNodes(htvic, state);
  }
  while (htvi = TreeView_GetNextSibling(g_hCtl, htvi));
}

// Counts the total number of siblings of a node (including the node itself).
// The first child node in a tree must be passed.
void TreeView_GetSiblingCount(HTREEITEM htvi, int &iCheckedCount, int &iDisabledCheckedCount, int &iCount)
{
  TVITEM tvi;
  do
  {
    (int*)iCount++;

    // Get node.
    tvi.mask = TVIF_HANDLE | TVIF_PARAM;
    tvi.hItem = htvi;
    TreeView_GetItem(g_hCtl, &tvi);

    // Is item checked...
    if (((POLDITEM)tvi.lParam)->iChecked)
    {
      if (((POLDITEM)tvi.lParam)->iDisableCheck)
        (int*)iDisabledCheckedCount++;
      else
        (int*)iCheckedCount++;
    }
  }
  while (htvi = TreeView_GetNextSibling(g_hCtl, htvi));
}

// Loops through parent nodes backwards to set their checked state.
void TreeView_SetParentNodeCheckState(HTREEITEM htvi)
{
  TVITEM tvi;
  POLDITEM poi;
  while (htvi = TreeView_GetParent(g_hCtl, htvi))
  {
    // Get number of siblings.
    int iCheckedCount = 0, iDisabledCheckedCount = 0, iCount = 0;
    TreeView_GetSiblingCount(TreeView_GetChild(g_hCtl, htvi), iCheckedCount, iDisabledCheckedCount, iCount);

    tvi.mask = TVIF_HANDLE | TVIF_PARAM;
    tvi.hItem = htvi;
    TreeView_GetItem(g_hCtl, &tvi);
    poi = (POLDITEM)tvi.lParam;

    // Manage parent node states when using the check bitmap.
    if (g_iCheckBoxes == 2)
    {
      // Uncheck parent node.
      if ((iCheckedCount + iDisabledCheckedCount) == 0)
      {
        if (poi->iDisableCheck == 1)
          tvi.state = TVIS_IMAGE_DISUNCHECKED;
        else if (poi->iDisableCheck == 2)
          tvi.state = TVIS_IMAGE_BLANK;
        else
          tvi.state = TVIS_IMAGE_UNCHECKED;
        poi->iChecked = FALSE;
      }
      // Partially check parent node.
      else if ((iCheckedCount + iDisabledCheckedCount) != iCount)
      {
        if (poi->iDisableCheck == 1)
          tvi.state = TVIS_IMAGE_DISCHECKED;
        else if (poi->iDisableCheck == 2)
          tvi.state = TVIS_IMAGE_BLANK;
        else
          tvi.state = TVIS_IMAGE_PCHECKED;

        if (iCheckedCount)
          poi->iChecked = 3;
        else
          poi->iChecked = 2;
      }
      // Check parent node.
      else
      {
        if (poi->iDisableCheck == 1)
          tvi.state = TVIS_IMAGE_DISCHECKED;
        else if (poi->iDisableCheck == 2)
          tvi.state = TVIS_IMAGE_BLANK;
        else
          tvi.state = TVIS_IMAGE_CHECKED;
        poi->iChecked = TRUE;
      }
    }
    // Manage parent node states when not using the check bitmap.
    else
    {
      // Uncheck parent node.
      if (iCheckedCount != iCount)
      {
        tvi.state = TVIS_UNCHECKED;
        poi->iChecked = FALSE;
      }
      // Check parent node.
      else
      {
        tvi.state = TVIS_CHECKED;
        poi->iChecked = TRUE;
      }
    }

    tvi.mask |= TVIF_STATE;
    tvi.stateMask = TVIS_STATEIMAGEMASK;
    TreeView_SetItem(g_hCtl, &tvi);
  }
}

// Loops through all nodes to set the check state of all parent nodes.
void TreeView_SetAllParentNodeCheckState(HTREEITEM htvi)
{
  HTREEITEM htvic;
  do
  {
    if (htvic = TreeView_GetChild(g_hCtl, htvi))
      TreeView_SetAllParentNodeCheckState(htvic);
    htvic = htvi;
  }
  while (htvi = TreeView_GetNextSibling(g_hCtl, htvi));
  TreeView_SetParentNodeCheckState(htvic);
}

// Check or uncheck all nodes in the tree view.
void TreeView_SetAllNodesCheckState(HTREEITEM htvi, BOOL state)
{
  HTREEITEM htvic;
  TVITEM tvi;
  POLDITEM poi;
  do
  {
    htvic = TreeView_GetChild(g_hCtl, htvi);
    if ((g_bParentCheck && !htvic) || !g_bParentCheck)
    {
      tvi.mask = TVIF_HANDLE | TVIF_PARAM;
      tvi.hItem = htvi;
      TreeView_GetItem(g_hCtl, &tvi);
      poi = (POLDITEM)tvi.lParam;

      if (!poi->iDisableCheck)
      {
        tvi.mask |= TVIF_STATE;
        tvi.stateMask = TVIS_STATEIMAGEMASK;

        poi->iChecked = state;
        if (state)
          tvi.state = (g_iCheckBoxes == 2 ? TVIS_IMAGE_CHECKED   : TVIS_CHECKED);
        else
          tvi.state = (g_iCheckBoxes == 2 ? TVIS_IMAGE_UNCHECKED : TVIS_UNCHECKED);

        TreeView_SetItem(g_hCtl, &tvi);
      }
    }

    if (htvic)
      TreeView_SetAllNodesCheckState(htvic, state);
  }
  while (htvi = TreeView_GetNextSibling(g_hCtl, htvi));
}

// Check all nodes that have iChecked = TRUE set.
void TreeView_CheckNodesToBeChecked(HTREEITEM htvi)
{
  HTREEITEM htvic;
  TVITEM tvi;
  POLDITEM poi;
  do
  {
    htvic = TreeView_GetChild(g_hCtl, htvi);
    if ((g_bParentCheck && !htvic) || !g_bParentCheck)
    {
      tvi.mask = TVIF_PARAM | TVIF_HANDLE;
      tvi.hItem = htvi;
      TreeView_GetItem(g_hCtl, &tvi);
      poi = (POLDITEM)tvi.lParam;

      if (poi->iDisableCheck && (g_iCheckBoxes == 2))
      {
        if (poi->iDisableCheck == 2)
          tvi.state = TVIS_IMAGE_BLANK;
        else
        {
          if (poi->iChecked == TRUE)
            tvi.state = TVIS_IMAGE_DISCHECKED;
          else
            tvi.state = TVIS_IMAGE_DISUNCHECKED;
        }
      }
      else
      {
        if (poi->iChecked == TRUE)
          tvi.state = (g_iCheckBoxes == 2 ? TVIS_IMAGE_CHECKED   : TVIS_CHECKED);
        else
          tvi.state = (g_iCheckBoxes == 2 ? TVIS_IMAGE_UNCHECKED : TVIS_UNCHECKED);
      }

      tvi.mask = TVIF_STATE | TVIF_HANDLE;
      tvi.stateMask = TVIS_STATEIMAGEMASK;
      TreeView_SetItem(g_hCtl, &tvi);
    }

    if (htvic)
      TreeView_CheckNodesToBeChecked(htvic);
  }
  while (htvi = TreeView_GetNextSibling(g_hCtl, htvi));
}

// Get number of checked nodes.
int TreeView_IsNodeChecked(HTREEITEM htvi)
{
  HTREEITEM htvic;
  TVITEM tvi;
  do
  {
    tvi.mask = TVIF_PARAM | TVIF_HANDLE;
    tvi.hItem = htvi;
    TreeView_GetItem(g_hCtl, &tvi);

    if (((POLDITEM)tvi.lParam)->iChecked == TRUE)
      return TRUE;

    if (htvic = TreeView_GetChild(g_hCtl, htvi))
      if (TreeView_IsNodeChecked(htvic))
        return TRUE;
  }
  while (htvi = TreeView_GetNextSibling(g_hCtl, htvi));
  return FALSE;
}

// Frees memory allocated for each tree view node.
void TreeView_FreeParams(HTREEITEM htvi)
{
  HTREEITEM htvic;
  TVITEM tvi;
  POLDITEM poi;
  tvi.mask = TVIF_PARAM;
  do
  {
    // Get lParam.
    tvi.hItem = htvi;
    TreeView_GetItem(g_hCtl, &tvi);

    // Get OLDITEM data.
    poi = (POLDITEM)tvi.lParam;

    // Free memory allocated.
    GlobalFree(poi->pszOldText);
    GlobalFree(poi);

    // Next child node.
    if (htvic = TreeView_GetChild(g_hCtl, htvi))
      TreeView_FreeParams(htvic);
  }
  while (htvi = TreeView_GetNextSibling(g_hCtl, htvi));
}

// Handles the parent dialog for list view dialog.
static BOOL CALLBACK TreeView_ParentDlgProc(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
  static HTREEITEM htvi;
  static TVITEM tvi;
  static POLDITEM poi;
  static BOOL bRes = FALSE;
  static int i;
  
  PTCHAR pszValue = (PTCHAR)LocalAlloc(LPTR, sizeof(TCHAR)*g_stringsize);

  if (uMsg == WM_NOTIFY_OUTER_NEXT && wParam == 1)
  {
    pushstring(OUT_ENDSTACK);

    // Save the tree view state.
    if (htvi = TreeView_GetRoot(g_hCtl))
      TreeView_SaveState(htvi);

    // If items can be selected, return
    // the selected item on the stack.
    if (!g_bNoItemSelect && !g_iCheckBoxes)
    {
      if ((tvi.hItem = TreeView_GetSelection(g_hCtl)))
      {

        // Add item text to stack.
        if (g_bReturnItemText)
        {
          tvi.mask = TVIF_TEXT | TVIF_HANDLE;
          tvi.pszText = pszValue;
          tvi.cchTextMax = g_stringsize;
          TreeView_GetItem(g_hCtl, &tvi);
          pushstring(tvi.pszText);
        }
        // Add item number to stack.
        else
        {
          tvi.mask = TVIF_PARAM | TVIF_HANDLE;
          TreeView_GetItem(g_hCtl, &tvi);
          poi = (POLDITEM)tvi.lParam;
          wsprintf(pszValue, TEXT("%i"), poi->iOldIndex);
          pushstring(pszValue);
        }
      }
    }
  }

  LocalFree(pszValue);

  bRes = CallWindowProc((long (__stdcall *)(struct HWND__ *,unsigned int,unsigned int,long))ParentDlgProcOld,hWnd,uMsg,wParam,lParam);
  if (uMsg == WM_NOTIFY_OUTER_NEXT && !bRes)
  {
    if (wParam == -1)
      g_is_back = TRUE;
    else if (wParam == NOTIFY_BYE_BYE)
      g_is_cancel = TRUE;
    g_done = TRUE;
    PostMessage(g_hDialog, WM_CLOSE, 0, 0);
  }
  return bRes;
}

// Handles TreeView dialog.
static LRESULT CALLBACK TreeView_DlgProc(HWND hWndDlg, UINT uMsg, WPARAM wParam, LPARAM lParam)
{
  static HWND hNext = GetDlgItem(g_hWndParent, IDC_NEXT);
  static BOOL bToggleNext = TRUE,
              bDialogShown = FALSE;
	static TCHAR szSectionName[32],
               szKeyName[32];
  static int  iValue,
              iSectionCount,
              iKeyCount,
              ihtviListCount,
              ihtviListIndex;
  static HIMAGELIST himlIcons,
                    himlState;
  static TVITEM         tvi;
  static TVITEM         tvip;
  static HTREEITEM      htvi;
  static HTREEITEM      htviList[8];
  static TVINSERTSTRUCT tviins;
  static DWORD lStyle = 0;
  static HICON hIcon;

  PTCHAR pszValue = (PTCHAR)LocalAlloc(LPTR, sizeof(TCHAR)*g_stringsize);
  BOOL bResult = FALSE;

	switch (uMsg)
	{
  case WM_SHOWWINDOW:
    {
      if (g_iCheckBoxes && !bDialogShown)
      {
        htvi = TreeView_GetRoot(g_hCtl);

        // Check all nodes to be checked (iChecked == TRUE).
        TreeView_CheckNodesToBeChecked(htvi);

        // Set the state of all parent nodes.
        if (g_bParentCheck)
          TreeView_SetAllParentNodeCheckState(htvi);
      }

      // Disable next button.
      if (bToggleNext)
        if (!TreeView_IsNodeChecked(TreeView_GetRoot(g_hCtl)))
          EnableWindow(hNext, FALSE);

      bDialogShown = TRUE;
    }
  break;
	case WM_INITDIALOG:

    g_hCtl = GetDlgItem(hWndDlg, IDC_LIST);
    lStyle = GetWindowLongPtr(g_hCtl, GWL_STYLE);
    {
      // Disable the next button and re-enable when a item is selected or checked?
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_NEXTTOGGLE, 0) == 0)
        bToggleNext = FALSE;

      // Do not allow item selection.
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_NOITEMSELECT, 0) == 1)
        g_bNoItemSelect = TRUE;

      // Return selected/checked items by item text rather than item number.
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_RETURNITEMTEXT, 0) == 1)
        g_bReturnItemText = TRUE;

      // Parent nodes uncheck child nodes when checked.
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_PARENTCHECK, 0))
        g_bParentCheck = TRUE;

      // Add icon files to image list.
      if (himlIcons = GetIcons())
        TreeView_SetImageList(g_hCtl, himlIcons, TVSIL_NORMAL);

      // Add check boxes to the list view.
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_CHECKBOXES, 0))
      {
        g_iCheckBoxes = TRUE;

        // Use NSIS check bitmap for state image list.
        if (ReadINIStr(pszValue, SEC_SETTINGS, KEY_USECHECKBITMAP, TEXT("1")) && lstrcmp(pszValue, TEXT("0")) != 0)
        {
          if (himlState = GetStateImage(pszValue))
          {
            TreeView_SetImageList(g_hCtl, himlState, TVSIL_STATE);
            TreeView_SetItemHeight(g_hCtl, 16);
          }
          else
            lStyle |= TVS_CHECKBOXES;
        }
        else
          lStyle |= TVS_CHECKBOXES;
      }

      // All tree nodes have editable labels.
      if (ReadINIInt(iValue, SEC_SETTINGS, KEY_LABELEDIT, 0))
      {
        g_bLabelEdit = TRUE;
        lStyle |= TVS_EDITLABELS;
      }

      // Add tree nodes.
      ihtviListCount = 1;
      wsprintf(szSectionName, TEXT("%s %i"), SEC_ITEM, iSectionCount = 1);
      while (ReadINIStr(pszValue, szSectionName, KEY_TEXT, TEXT("")))
      {
        // Set general item properties.
        tvi.mask = TVIF_HANDLE | TVIF_TEXT | TVIF_PARAM | TVIF_STATE;
        tvi.pszText = pszValue;
        tvi.cchTextMax = g_stringsize;
        tvi.state = 0;
        tvi.stateMask = 0;

        // Store original item info in item tag.
        tvi.lParam = (LPARAM)CreateOldItem(iSectionCount, szSectionName, pszValue);

        // Have an image list present?
        if (himlIcons)
        {
          // Setting an icon on the node?
          if (ReadINIInt(iValue, szSectionName, KEY_ICONINDEX, 0) != 0)
          {
            tvi.mask |= TVIF_IMAGE | TVIF_SELECTEDIMAGE;
            tvi.iImage = iValue-1; // Conversion to zero-based.
            tvi.iSelectedImage = iValue-1; // Conversion to zero-based.
          }
        }

        // No position set, just insert under last inserted node!
        if (ReadINIInt(ihtviListIndex, szSectionName, KEY_POSITION, 0) != 0)
        {
          ihtviListCount++;
          if (ihtviListIndex > ihtviListCount)
            ihtviListIndex = ihtviListCount;
          if (ihtviListIndex-2 < 0)
            tviins.hParent = TVI_ROOT;
          else
            tviins.hParent = htviList[ihtviListIndex-2];
        }
        else ihtviListIndex = ihtviListCount;

        // Bold tree node?
        if (ReadINIInt(iValue, szSectionName, KEY_BOLDTEXT, 0) == 1)
        {
          tvi.state |= TVIS_BOLD;
          tvi.stateMask |= TVIS_BOLD;
        }

        // Expand tree node?
        if (ReadINIInt(iValue, szSectionName, KEY_EXPANDED, 0) == 1)
        {
          tvi.state |= TVIS_EXPANDED;
          tvi.stateMask |= TVIS_EXPANDED;
        }

        // Insert the new node.
        tviins.item = tvi;
        htvi = TreeView_InsertItem(g_hCtl, &tviins);
        htviList[ihtviListIndex-1] = htvi;

        // Next item.
        wsprintf(szSectionName, TEXT("%s %i"), SEC_ITEM, ++iSectionCount);
      }

    }
    SetCommonTexts(hWndDlg);
    SetWindowLongPtr(g_hCtl, GWL_STYLE, lStyle);

  break;
  case WM_CONTEXTMENU:
    {
      // Display a popup menu with check/uncheck all options.
      if ((wParam == (WPARAM)g_hCtl))
      {
        // Only display the menu if there is at least 1 node
        // and check boxes are enabled.
        if (g_iCheckBoxes && TreeView_GetCount(g_hCtl))
        {
          HMENU hMenu = CreatePopupMenu();
          POINT pt;
          // Add the two options.
          AppendMenu(hMenu, MF_STRING, 1, TEXT_CHECKALL);
          AppendMenu(hMenu, MF_STRING, 2, TEXT_UNCHECKALL);
          // Get the position to display the popup menu.
          if (lParam == ((UINT)-1))
          {
            RECT r;
            GetWindowRect(g_hCtl, &r);
            pt.x = r.left;
            pt.y = r.top;
          }
          else
          {
            pt.x = GET_X_LPARAM(lParam);
            pt.y = GET_Y_LPARAM(lParam);
          }
          // Display the menu and detect which item was selected.
          switch (TrackPopupMenu(hMenu, TPM_NONOTIFY|TPM_RETURNCMD, pt.x, pt.y, 0, g_hCtl, 0))
          {
            case 1:
              TreeView_SetAllNodesCheckState(TreeView_GetRoot(g_hCtl), TRUE);
            break;
            case 2:
              TreeView_SetAllNodesCheckState(TreeView_GetRoot(g_hCtl), FALSE);
            break;
          }

          // Set the state of all parent nodes.
          if (g_bParentCheck)
            TreeView_SetAllParentNodeCheckState(htvi);
        }
      }
    }
  break;
  case WM_NOTIFY:
    {
      NMHDR* pnmh = (NMHDR*)lParam;
      switch (pnmh->code)
      {
      case TVN_SELCHANGED:
        {
          // Unselect any selected items.
          if (!g_bLabelEdit && g_bNoItemSelect)
            TreeView_SelectItem(g_hCtl, NULL);
        }
      break;
      case TVN_BEGINLABELEDIT:
        {
          NMTVDISPINFO* ptvdi = (NMTVDISPINFO*)lParam;
          POLDITEM poi;
          // Get OLDITEM info.
          ptvdi->item.mask = TVIF_PARAM | TVIF_HANDLE;
          TreeView_GetItem(g_hCtl, &ptvdi->item);
          poi = (POLDITEM)ptvdi->item.lParam;
          // Find out if the item label can be edited.
          if (poi->bDisableEdit)
          {
            int i;
            // No, so press tab to cancel edit.
            for (i=0; i<4; i++)
            {
              // Press it 4 times to tab back to TV control.
              keybd_event(VK_TAB, 0, 0, 0);
              keybd_event(VK_TAB, 0, KEYEVENTF_KEYUP, 0);
            }
          }
          bResult = TRUE;
        }
      break;
      case TVN_ENDLABELEDIT:
        {
          NMTVDISPINFO* ptvdi = (NMTVDISPINFO*)lParam;
          // Label is being edited.
          if (ptvdi->item.pszText != NULL)
          {
            ptvdi->item.mask = TVIF_TEXT;
            TreeView_SetItem(g_hCtl, &ptvdi->item);
          }
          bResult = TRUE;
        }
      break;
      // Tree view was clicked on.
      case NM_DBLCLK:
      case NM_CLICK:
        {
          // The point at which the click took place
          // so we can perform a 'hit test'.
          POINT p;
          TVHITTESTINFO tvhti;

          // Do not continue if check boxes are not enabled.
          if (!g_iCheckBoxes)
            break;

          // Get mouse cursor position in tree view.
          GetCursorPos(&p);
          ScreenToClient(g_hCtl, &p);
          tvhti.pt.x = p.x;
          tvhti.pt.y = p.y;

          // Get item user just clicked on.
          if ((tvi.hItem = TreeView_HitTest(g_hCtl, &tvhti)) != NULL)
          {
            POLDITEM poi;
            // Get item.
            tvi.mask = TVIF_PARAM | TVIF_HANDLE;
            TreeView_GetItem(g_hCtl, &tvi);
            // Get OLDITEM info.
            poi = (POLDITEM)tvi.lParam;

            tvi.stateMask = TVIS_STATEIMAGEMASK;

            // Clicked on a check box?
            if (tvhti.flags & TVHT_ONITEMSTATEICON)
            {
              // Don't do anything if we are using the check bitmap
              // and the this check box is disabled.
              if (!poi->iDisableCheck && (g_iCheckBoxes == 2))
              {

                // Manage the parent node.
                if (g_bParentCheck && (htvi = TreeView_GetChild(g_hCtl, tvi.hItem)))
                {
                  TreeView_CheckChildNodes(htvi, (poi->iChecked == 3 || poi->iChecked == 1 ? FALSE : TRUE));
                  TreeView_SetParentNodeCheckState(htvi);
                }
                else
                {
                  // Check box will be unchecked.
                  if (poi->iChecked == TRUE)
                  {
                    tvi.state = TVIS_IMAGE_UNCHECKED;
                    poi->iChecked = FALSE;
                  }
                  else
                  {
                    tvi.state = TVIS_IMAGE_CHECKED;
                    poi->iChecked = TRUE;
                  }
                  tvi.mask |= TVIF_STATE;
                  TreeView_SetItem(g_hCtl, &tvi);
                  
                  TreeView_SetParentNodeCheckState(tvi.hItem);
                }

                // Disable next button.
                if (bToggleNext)
                {
                  if (TreeView_IsNodeChecked(TreeView_GetRoot(g_hCtl)))
                    EnableWindow(hNext, TRUE);
                  else
                    EnableWindow(hNext, FALSE);
                }
              }
              // If we aren't using the check bitamp...
              else if (g_iCheckBoxes == 1)
              {
                // The node check state cannot be changed...
                if (poi->iDisableCheck)
                {
                  // Check box must stay checked.
                  if (poi->iChecked)
                  {
                    if (pnmh->code == NM_DBLCLK)
                      tvi.state = TVIS_CHECKED;
                    else
                      tvi.state = TVIS_UNCHECKED;
                  }
                  // Check box must stay unchecked.
                  else
                  {
                    if (pnmh->code == NM_DBLCLK)
                      tvi.state = TVIS_UNCHECKED;
                    else
                      tvi.state = TVIS_CHECKED;
                  }

                  tvi.mask = TVIF_STATE | TVIF_HANDLE;
                }
                else
                {
                  tvi.mask = TVIF_PARAM | TVIF_HANDLE;

                  // Check box is to be unchecked.
                  if (poi->iChecked)
                    poi->iChecked = FALSE;
                  // Check box is to be checked.
                  else
                    poi->iChecked = TRUE;

                  // Manage the parent node.
                  if (g_bParentCheck)
                  {
                    if (htvi = TreeView_GetChild(g_hCtl, tvi.hItem))
                    {
                      TreeView_CheckChildNodes(htvi, poi->iChecked);
                      TreeView_SetParentNodeCheckState(htvi);
                    }
                    else
                      TreeView_SetParentNodeCheckState(tvi.hItem);

                    // Correct the parent node state as it is about to be toggled.
                    if (poi->iChecked)
                    {
                      if (pnmh->code == NM_DBLCLK)
                        tvi.state = TVIS_CHECKED;
                      else
                        tvi.state = TVIS_UNCHECKED;
                    }
                    else
                    {
                      if (pnmh->code == NM_DBLCLK)
                        tvi.state = TVIS_UNCHECKED;
                      else
                        tvi.state = TVIS_CHECKED;
                    }

                    tvi.mask |= TVIF_STATE;
                  }

                }
                TreeView_SetItem(g_hCtl, &tvi);
              }
            }
            // There is a bug where double clicking on the left margin of the state image
            // will change the state icon incorrectly. This corrects the effects of the bug.
            else
            {
              if (poi->iDisableCheck && (g_iCheckBoxes == 2))
              {
                if (poi->iDisableCheck == 2)
                  tvi.state = TVIS_IMAGE_BLANK;
                else
                {
                  if (poi->iChecked)
                    tvi.state = TVIS_IMAGE_DISCHECKED;
                  else
                    tvi.state = TVIS_IMAGE_DISUNCHECKED;
                }
              }
              else
              {
                if (poi->iChecked == TRUE)
                  tvi.state = (g_iCheckBoxes == 2 ? TVIS_IMAGE_CHECKED : TVIS_CHECKED);
                else if ((poi->iChecked >= 2) && (g_iCheckBoxes == 2))
                  tvi.state = TVIS_IMAGE_PCHECKED;
                else
                  tvi.state = TVIS_IMAGE_UNCHECKED;
              }
              tvi.mask = TVIF_HANDLE | TVIF_STATE;
              TreeView_SetItem(g_hCtl, &tvi);
            }
          }
        }
      break;
      }
    }
  break;
  case WM_DESTROY:
    {
      TreeView_FreeParams(TreeView_GetRoot(g_hCtl));
    }
  break;
  case WM_CTLCOLORSTATIC:
  case WM_CTLCOLOREDIT:
  case WM_CTLCOLORDLG:
  case WM_CTLCOLORBTN:
  case WM_CTLCOLORLISTBOX:
    // Let the NSIS window handle colours, it knows best.
    bResult = SendMessage(g_hWndParent, uMsg, wParam, lParam);
  }

  LocalFree(pszValue);
	return bResult;
}

void CreateDialogs()
{
  TCHAR szDialog[16];
  popstring(g_szINIFilePath);
  g_done = TRUE;

  // Find out which page to use.
  ReadINIStr(szDialog, SEC_SETTINGS, KEY_TYPE, TEXT("ListView"));

  // Which page to use?
  if (lstrcmpi(szDialog, DLG_LISTVIEW) == 0)
  {
    g_iDialog = 0;
    g_hDialog = CreateDialog(g_hInstance, MAKEINTRESOURCE(IDD_LISTVIEW), g_hWndParent, (DLGPROC)ListView_DlgProc);
  }
  else
  {
    g_iDialog = 1;
    g_hDialog = CreateDialog(g_hInstance, MAKEINTRESOURCE(IDD_TREEVIEW), g_hWndParent, (DLGPROC)TreeView_DlgProc);
  }
}

// Plugin callback for new plugin API.
static UINT_PTR PluginCallback(enum NSPIM msg)
{
  return 0;
}

NSISFUNC(InitDialog)
{
  DLL_INIT();

  if (!g_bInitDialog)
  {
    TCHAR szHWND[32];
    CreateDialogs();

    // Return page HWND.
    wsprintf(szHWND, TEXT("%d"), g_hDialog);
    pushstring(szHWND);

    g_bInitDialog = TRUE;

  }
  else
    pushstring(OUT_ERROR);
}

NSISFUNC(Show)
{
  DLL_INIT();

  if (g_bInitDialog)
  {
    g_bInitDialog = FALSE;
    ShowDialog();
  }
  else
    pushstring(OUT_ERROR);
}

NSISFUNC(Dialog)
{
  DLL_INIT();

  if (!g_bInitDialog)
  {
    CreateDialogs();
    ShowDialog();
  }
  else
    pushstring(OUT_ERROR);
}

// Entry point for DLL.
BOOL WINAPI DllMain(HINSTANCE hInst, ULONG ul_reason_for_call, LPVOID lpReserved)
{
  g_hInstance=hInst;
  return TRUE;
}