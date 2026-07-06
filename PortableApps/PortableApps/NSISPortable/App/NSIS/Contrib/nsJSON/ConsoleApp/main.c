#include <Windows.h>

int main()
{
	CHAR szInBuffer[32], szOutBuffer[128];
	HANDLE hStdIn = GetStdHandle(STD_INPUT_HANDLE), hStdOut = GetStdHandle(STD_OUTPUT_HANDLE);
	DWORD dwBytesRead, dwBytesWritten;
	int i, cchOutBuffer;

	if (ReadFile(hStdIn, szInBuffer, sizeof(szInBuffer), &dwBytesRead, NULL) && dwBytesRead)
	{
		for (i = lstrlenA(szInBuffer) - 1; i >= 0 && (szInBuffer[i] == '\n' || szInBuffer[i] == '\r' || szInBuffer[i] == '\t' || szInBuffer[i] == ' '); i--)
			szInBuffer[i] = '\0';

		// Pretend to do some work.
		Sleep(5000);

		// Return JSON.
		//cchOutBuffer = wsprintfA(szOutBuffer, "{\"Input\": \"%s\", \"Output\": \"blah!\"}", szInBuffer);

		// Or return a string.
		lstrcpyA(szOutBuffer, "blah!!!!");
		cchOutBuffer = lstrlenA(szOutBuffer);

		WriteFile(hStdOut, szOutBuffer, cchOutBuffer, &dwBytesWritten, NULL);
	}

	ExitProcess(0);
	return 0;
}