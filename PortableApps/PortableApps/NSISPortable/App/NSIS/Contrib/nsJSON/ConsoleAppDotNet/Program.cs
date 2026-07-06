using System;
using System.Threading;

namespace ConsoleAppDotNet
{
    class Program
    {
        static void Main()
        {
            string input = Console.ReadLine();
            if (input == null)
                return;

            input = input.Trim();
            if (input == string.Empty)
                return;

            // Pretend to do some work.
            Thread.Sleep(TimeSpan.FromSeconds(5));

            // Return JSON.
            Console.Write("{{\"Input\": \"{0}\", \"Output\":\"blah!\"}}", input);
        }
    }
}
