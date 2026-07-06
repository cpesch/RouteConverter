nsJSON NSIS plug-in

Author:  Stuart 'Afrow UK' Welch
Date:    21st November 2017
Version: 1.1.1.0

A JSON (JavaScript Object Notation) parser, manipulator and generator
plug-in for NSIS.

See Examples\nsJSON\*.

------------------------------------------------------------------------
About JSON
------------------------------------------------------------------------

  See http://www.json.org/ for information about JSON along with the
  syntax and string escape sequences.

------------------------------------------------------------------------
Important information
------------------------------------------------------------------------

  All plug-in functions set the NSIS error flag on error. For functions
  that return a value on the stack, no item will be returned if the
  error flag has been set. You can use IfErrors or ${If} ${Errors} to
  check that a call has succeeded before Pop-ping a value, if any.

  The plug-in supports all escape sequences in string values:
  \r, \n, \t, \b, \f, \", \\ and \uXXXX

  [NodePath], used throughout this readme, is any space-delimited list
  of mixed node keys and indices (prefixed with /index). For example:

  {
    "node1": {
      "node2": false,
      "node3": [ "Stuart", 1, { "node4": "Welch", "node5": "" }, 32.5, [ 0, "test", false ] ]
    }
  }

  This is a JSON snippet. "node2" is a child node of "node1". "node2"
  has a Boolean value of false. "node3" is an array (denoted by square
  brackets). The 3rd element within the array is another node. The 5th
  element within the array is another array.

  These are examples of node paths within the JSON snippet:

  "node1" "node2"
  -> The path for "node2" (value: false)
  -> nsJSON::Get "node1" "node2" /end

  "node1" "node3" /index 0
  -> The path for the first array element in "node3" (value: "Stuart")
  -> nsJSON::Get "node1" "node3" /index 0 /end

  "node1" "node3" /index 2 "node4"
  -> The path for "node4" (value: "Welch")
  -> nsJSON::Get "node1" "node3" /index 3 "node4" /end

  /index 0 "node3" /index 2 /index 0
  -> Also the path for "node4" (value: "Welch")
  -> nsJSON::Get /index 0 /index 1 /index 3 /index 0 /end

  "node1" "node3" /index 4 /index -2
  -> The path for value "test" in the array; note the negative index
  -> nsJSON::Get "node1" "node3" /index 4 /index -2 /end

  Node names containing double quotes are automatically escaped. If no
  node path is given, the root node is used.

  Multiple JSON trees can be manipulated at the same time. Add
  /tree Tree (where Tree is the tree name) before all other plug-in
  arguments to specify which JSON tree you are manipulating.

------------------------------------------------------------------------
Reading JSON files
------------------------------------------------------------------------

  nsJSON::Set [/tree Tree] [NodePath] /file [/unicode] "path\to\input.json"

  Loads the JSON from the given file into the given node. Specify
  /unicode if the input file is Unicode.

------------------------------------------------------------------------

  nsJSON::Set [/tree Tree] /file [/unicode] "path\to\input.json"

  Loads the JSON from the given file into memory, overwriting any
  existing JSON tree. Specify /unicode if the input file is Unicode.

------------------------------------------------------------------------
JSON from HTTP web requests
------------------------------------------------------------------------

  nsJSON::Set [/tree Tree] [NodePath] /http ConfigTree

  Executes an HTTP web request and loads the JSON response into the
  given node. The ConfigTree JSON tree specifies the web request
  configuration to execute, described using JSON. Its possible values
  are as follows...

  "Url": "http://..."
  - The web request URL. This is required.

  "Verb": "GET"
  - The request verb, such as GET or POST. Default is GET.

  "Params": "..."
  - Optional parameters to append to the URL (after ?).

  "ParamsType": "..."
  - The parameters type. Possible values are...

    Empty string/omitted (default)
    - Parameters are given as JSON and will be sent as a standard
      key/value pair string. Special characters will be URL-encoded
      automatically. Arrays will be serialized as multiple keys of the
      same name with a [] suffix.

      For example {"a": "Value1", "b": true, "c": "Value3"} becomes
      a=Value1&b&c=Value3, and {"Array": [1, 2]} becomes
      Array[]=1&Array[]=2.

    "Raw"
    - Raw parameters are given as a string.

  "Data": "..."
  - Optional request data to send (typically used with POST).

  "DataType": "..."
  - The request data type. Possible values are...

    Empty string/omitted (default)
    - Data is given as JSON and will be sent as a standard POST
      key/value pair string. Special characters will be URL-encoded
      automatically. Arrays will be serialized as multiple keys of the
      same name with a [] suffix.

      For example {"a": "Value1", "b": true, "c": "Value3"} becomes
      a=Value1&b&c=Value3, and {"Array": [1, 2]} becomes
      Array[]=1&Array[]=2.

    "JSON"
    - POST data is JSON and will be sent as-is (serialized JSON).

    "Raw"
    - Raw POST data is given as a string.

  "DataEncoding": "..."
  - Specifies the encoding to use for the POST data. Possible values
    are...

    Empty string/omitted (default)
    - Data is encoded as 1 byte per character.

    "Unicode"
    - Data is encoded as 2 bytes per character.

  "Headers": "..."
  - Additional headers to send, which will override these defaults...
    * "Content-Type: application/x-www-form-urlencoded" is sent if "Verb"
      is "POST" and "DataType" is "Raw".
    * "Content-Type: application/json" is sent if "Verb" is "POST" and
      "DataType" is "JSON".
    * "Accept-Encoding: gzip,deflate" is sent if "Decoding" is set to
      true.

    Specify headers as JSON, for example:
    {"Content-Type": "text/plain", "a": "b"}


  "Agent": "nsJSON NSIS plug-in/1.0.x.x"
  - The request agent string. Default is shown.

  "Decoding": false
  - Enables automatic GZIP decompression by sending the
    "Accept-Encoding: gzip,deflate" request header. If the server responds
    with a valid "Content-Encoding" header, the compressed data will be
    decompressed automatically.

  "AccessType": "..."
  - The connection access type. Possible values are...

    Empty string/omitted (default)
    - Direct access (bypasses any proxy).

    "PreConfig"
    - Use the internet configuration defined in the Windows registry.

    "Proxy"
    - Use defined proxy configuration (see below).

  "Proxy": { "Server": "...", "Bypass": "...", Username": "...", "Password": "..." }
  - Specifies the proxy configuration. The "Bypass" value is optional.

  "Username": "..."
  - Username for HTTP authentication.

  "Password": "..."
  - Password for HTTP authentication.

  "ConnectTimeout": "..."
  - The initial connection timeout. A value of 0XFFFFFFFF will
    disable the timeout.

  "SendTimeout": N
  - The request send timeout.

  "ReceiveTimeout": N
  - The response receive timeout.

  "UnicodeOutput": true
  - The response must be read as Unicode rather than ASCII.

  "RawOutput": true
  - The response must be read as raw text rather than JSON.

  "Async": true
  - Perform the request asynchronously. You must use the Wait function
    to wait or check if the request has finished.

  "UIAsync": true
  - For performing the request in a page callback function. This will
    ensure window messages are processed so that the UI does not become
    unresponsive.

  After the request has finished, the node specified by NodePath will
  contain the following values:

  "Output": ...
  - The HTTP response as JSON.

  "StatusCode": N
  - The HTTP status code.

  "ErrorCode": N
  - The WinINet/Win32 error code on error.

  "ErrorMessage": "..."
  - The WinINet/Win32 error message on error.

------------------------------------------------------------------------
JSON from console application execution
------------------------------------------------------------------------

  nsJSON::Set [/tree Tree] [NodePath] /exec ConfigTree

  Executes a console application and loads the output into the given
  node. The ConfigTree JSON tree specifies the console application to
  execute, described using JSON. Its possible values are as follows...

  "Path": "$INSTDIR\ConsoleApp.exe"
  - The path to the executable to be executed. This is required.

  "Arguments": ...
  - The command line arguments. The value can be a string or an array of
    strings.

  "WorkingDir": "..."
  - The working/current directory. If not specified, the installer's
    working directory will be used, which is $OUTDIR.

  "Input": ...
  - The input to write to STDIN. The value can be a string or an array
    of strings.

  "UnicodeInput": true
  - The standard input must be written as Unicode rather than ASCII.

  "UnicodeOutput": true
  - The standard output must be read as Unicode rather than ASCII.

  "RawOutput": true
  - The standard output must be read as raw text rather than JSON.

  "Async": true
  - Execute asynchronously. You must use the Wait function to wait or
    check if the process has finished.

  "UIAsync": true
  - For performing the execution in a page callback function. This will
    ensure window messages are processed so that the UI does not become
    unresponsive.

  After execution has finished, the node specified by NodePath will
  contain the following values:

  "Output": ...
  - The output of the console application, depending on the
    RawOutput setting.

  "ExitCode": X
  - The process exit code.

------------------------------------------------------------------------
Modifying JSON
------------------------------------------------------------------------

  nsJSON::Set [/tree Tree] [NodePath] /value "Value"

  Sets the value of the given node. The value can be any single value or
  it can be JSON code. The node will be created if it does not exist.

  --------------------------------------------------------------------

  nsJSON::Delete [/tree Tree] [NodePath] /end

  Deletes the given tree or node. /end must be added to the end of the
  list to prevent stack corruption.

  --------------------------------------------------------------------

  nsJSON::Quote [/unicode] [/always] Value
  Pop $Var

  Surrounds the given value with quotes if necessary and escapes any
  characters that require it. The quoted value will be returned on the
  stack.

  Optionally specify /unicode to escape non ASCII characters as well
  although it is perfectly legal to include Unicode characters in JSON.

  Specify /always to always surround the input string in quotes even if
  it is already quoted.

  --------------------------------------------------------------------

  nsJSON::Sort [/tree Tree] [NodePath] [/options Options] /end

  Sorts the given node. Add up the following values for the optional
  sort options:

    1 = Sort in descending order.
    2 = Use numeric sort.
    4 = Sort case sensitively.
    8 = Sort by keys rather than by values.
    16 = Sort all nodes in the tree recursively.

------------------------------------------------------------------------
Reading JSON values
------------------------------------------------------------------------

  nsJSON::Get [/tree Tree] [/noexpand] [NodePath] /end
  Pop $Var

  Gets the value of the given node. The value returned will be JSON code
  if the node is not a value-only node. Specify /noexpand when reading
  quoted string values to stop escape sequences being expanded. /end
  must be added to the end of the list to prevent stack corruption.

  --------------------------------------------------------------------

  nsJSON::Get [/tree Tree] /type [NodePath] /end
  Pop $Var

  Gets the value-type of the given node. It can be one of "node",
  "array", "string" (quoted strings), "value" (which is all non-string
  values such as integers, floats and Booleans) or an empty string if
  the node does not exist.

  --------------------------------------------------------------------

  nsJSON::Get [/tree Tree] /key [NodePath] /end
  Pop $Var

  Gets the name (key) of the given node.

  --------------------------------------------------------------------

  nsJSON::Get [/tree Tree] /keys [NodePath] /end
  Pop $VarKeyCount
  Pop $VarKey1
  Pop $VarKey2
  Pop $VarKeyN

  Gets the keys of the given node. $VarKeyCount will be the number of
  keys pushed onto the stack (to be removed via Pop).

  --------------------------------------------------------------------

  nsJSON::Get [/tree Tree] /exists [NodePath] /end
  Pop $Var

  Determines whether or not the given node exists. $Var will be "yes" or
  "no".

  --------------------------------------------------------------------

  nsJSON::Get [/tree Tree] /count [NodePath] /end
  Pop $Var

  Gets the number of child nodes for the given node or the number of
  elements if the node is an array.

  --------------------------------------------------------------------

  nsJSON::Get [/tree Tree] /isempty [NodePath] /end
  Pop $Var

  Determines whether or not the given node is empty. $Var will be "yes"
  or "no". An empty node is for example: "node": { } or "array": [ ].

------------------------------------------------------------------------
Generating JSON
------------------------------------------------------------------------

  nsJSON::Serialize [/tree Tree] [/format]
  Pop $Var

  Serializes the current JSON tree into $Var. Add /format to apply
  formatting to the output. The error flag is set if an error occurs.

  --------------------------------------------------------------------

  nsJSON::Serialize [/tree Tree] [/format] /file [/unicode] "path\to\output.json"

  Serializes the current JSON tree into the given file. Add /format to
  apply formatting to the output. Add /unicode to generate a Unicode
  output file (applies to both ANSI and Unicode NSIS).

------------------------------------------------------------------------
Waiting for asynchronous tasks to finish
------------------------------------------------------------------------

  nsJSON::Wait Tree [/timeout TimeoutInMilliseconds]
  Pop $Var

  Checks whether or not the asynchonous task that is being ran under the
  given JSON tree has finished.

  If /timeout is specified, the function will return after the
  specified number of milliseconds; after which $Var will be "wait" if
  the task has not finished; or an empty string otherwise.

  When /timeout is omitted, the function will wait indefinitely and
  nothing will be pushed onto the stack when the task finishes.

------------------------------------------------------------------------
Change log
------------------------------------------------------------------------

1.1.1.0 - 21st November 2017
* Fixed JSON with syntax errors still being parsed without setting the
  error flag.
* Fixed Set function not replacing the root value if the value was an
  array.
* Fixed Delete function not deleting the root node and tree.

1.1.0.9 - 9th August 2017
* Fixed access violation and stack overflow crashes in JSON_Delete, which
  could occur on plug-in unload, Delete function call or when overwriting
  existing nodes with the Set function.
* Fixed DoHttpWebRequest failing when POST data exceeded 2048 characters.
* Fixed DoCreateProcess failing to read process output when the output
  exceeded 1024 characters.
* Delete function will now delete a complete node tree if no path is
  given.

1.1.0.8 - 8th January 2017
* Added ErrorCode and ErrorMessage for HTTP requests for getting WinINet
  or Win32 errors.
* Added Sort function.

1.1.0.7 - 7th November 2016
* Fixed /index for adding array elements with the Set function.
* Fixed file name being left on the stack after Serialize with /file.
* Added access type, proxy, authentication and timeout options for HTTP
  requests.
* Added POST data encoding options for HTTP requests.
* HTTP request headers can be given as raw headers or as JSON key/value
  pairs.
* Fixed a memory leak in JSON_SerializeAlloc and EscapePostData.
* POST parameters given as a JSON array was not escaped using
  EscapePostData.

1.1.0.6 - 14th October 2016
* Output and ExitCode/StatusCode are always cleared for /exec and /http
  calls on the Set function.

1.1.0.5 - 10th August 2016
* Empty value keys ("") were not included in serialized JSON.
* Serialize function popped one extra value from the stack.

1.1.0.4 - 7th June 2016
* Fixed incorrect flags used for HTTP requests.
* Added support for negative indices.
* Added console application execution for input and output.
* Added support for asynchronous HTTP requests and console application
  execution.

1.1.0.3 - 4th March 2016
* Fixed crash for the Delete function when specifying a tree/path that
  does not exist.
* Fixed Unicode build.
* Added /keys switch to Get function.

1.1.0.2 - 9th December 2015
* Get function /type switch returns an empty string if the node does not
  exist.
* Added /always switch to Quote function which surrounds the input
  string in quotes even if it is already quoted.

1.1.0.1 - 23rd November 2015
* Added Quote function.
* Added amd64-unicode build.

1.1.0.0 - 19th April 2015
* Support for multiple JSON trees.
* JSON via HTTP web requests.

1.0.1.3 - 18th October 2014
* Added UTF-16LE BOM, UTF-16BE BOM and UTF-8 signature detection for
  input files (with UTF-16BE conversion to UTF-16LE).
* Fixed formatting errors for the Serialize function.
* Fixed closing bracket or curly brace not being included on Serialize
  to stack when not using /format.
* Moved plug-in DLLs to x86-ansi and x86-unicode respectively for NSIS
  3.0.

1.0.1.2 - 12th July 2014
* Fixed crash on serialization to file for node values larger than 64KB.
* Fixed crash on serialization to stack for JSON larger than
  NSIS_MAX_STRLEN. The JSON will now be truncated.

1.0.1.1 - 29th March 2014
* Fixed incorrect handling of escape character (\).

1.0.1.0 - 28th August 2012
* Added /unicode switch to the Serialize function. Output files for both
  plug-in builds are now encoded in ANSI by default.
* Removed the Parse function in favour of Set /file [/unicode].
* Added /type, /key, /exists, /count, /isempty to the Get function.
* Added /index switch for referencing nodes by index.

1.0.0.2 - 15th August 2012
* Fixed Unicode build parsing and serializing.

1.0.0.1 - 1st July 2012
* Fixed parsing of single digit numbers.
* Fixed Serialize not writing the output file when the stack isn't
  empty.

1.0.0.0 - 25th June 2012
* First version.

------------------------------------------------------------------------
License
------------------------------------------------------------------------

This plug-in is provided 'as-is', without any express or implied
warranty. In no event will the author be held liable for any damages
arising from the use of this plug-in.

Permission is granted to anyone to use this plug-in for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:

1. The origin of this plug-in must not be misrepresented; you must not
   claim that you wrote the original plug-in.
   If you use this plug-in in a product, an acknowledgment in the
   product documentation would be appreciated but is not required.
2. Altered versions must be plainly marked as such, and must not be
   misrepresented as being the original plug-in.
3. This notice may not be removed or altered from any distribution.