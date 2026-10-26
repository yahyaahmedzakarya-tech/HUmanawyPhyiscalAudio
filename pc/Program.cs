using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using NAudio.CoreAudioApi;
using NAudio.Wave;

const int Port = 5000;

// 1. Set up the USB tunnel (phone listens, PC connects)
RunAdb($"forward tcp:{Port} tcp:{Port}");

// 2. Pick the render device to loopback-capture
var enumerator = new MMDeviceEnumerator();
var devices = enumerator.EnumerateAudioEndPoints(DataFlow.Render, DeviceState.Active).ToList();
for (int i = 0; i < devices.Count; i++)
    Console.WriteLine($"[{i}] {devices[i].FriendlyName}");
Console.Write("Pick the HUmanawy device number: ");
var device = devices[int.Parse(Console.ReadLine()!)];

// 3. Capture and stream, reconnecting if the phone side drops
while (true)
{
    try { Stream(device); }
    catch (Exception e) { Console.WriteLine("Link lost: " + e.Message); }
    Console.WriteLine("Retrying in 2s...");
    Thread.Sleep(2000);
}

static void Stream(MMDevice device)
{
    using var client = new TcpClient { NoDelay = true };
    client.Connect(IPAddress.Loopback, Port);
    using var net = client.GetStream();

    using var capture = new WasapiLoopbackCapture(device);
    var fmt = capture.WaveFormat; // usually 32-bit float, so convert to 16-bit
    Console.WriteLine($"Capturing {fmt.SampleRate} Hz, {fmt.Channels} ch, {fmt.Encoding}");

    // Handshake: sampleRate, channels, bitsPerSample (big-endian int32 x3)
    WriteInt(net, fmt.SampleRate);
    WriteInt(net, fmt.Channels);
    WriteInt(net, 16);

    var failed = new ManualResetEventSlim();
    Exception? error = null;

    capture.DataAvailable += (s, e) =>
    {
        if (e.BytesRecorded == 0 || failed.IsSet) return;
        try
        {
            byte[] pcm16 = FloatTo16(e.Buffer, e.BytesRecorded, fmt);
            net.Write(pcm16, 0, pcm16.Length);
        }
        catch (Exception ex) { error = ex; failed.Set(); }
    };
    capture.RecordingStopped += (s, e) => { error ??= e.Exception; failed.Set(); };

    capture.StartRecording();
    failed.Wait();
    capture.StopRecording();
    throw error ?? new Exception("Capture stopped");
}

static byte[] FloatTo16(byte[] src, int count, WaveFormat fmt)
{
    // WASAPI loopback shared mode is IEEE float 32 in practice
    int samples = count / 4;
    var dst = new byte[samples * 2];
    for (int i = 0; i < samples; i++)
    {
        float f = BitConverter.ToSingle(src, i * 4);
        f = Math.Clamp(f, -1f, 1f);
        short s = (short)(f * 32767);
        dst[i * 2] = (byte)(s & 0xFF);        // little-endian PCM16
        dst[i * 2 + 1] = (byte)((s >> 8) & 0xFF);
    }
    return dst;
}

static void WriteInt(Stream s, int v)
{
    s.Write(new[] { (byte)(v >> 24), (byte)(v >> 16), (byte)(v >> 8), (byte)v }, 0, 4);
}

static void RunAdb(string args)
{
    var p = Process.Start(new ProcessStartInfo("adb", args)
    {
        UseShellExecute = false,
        RedirectStandardOutput = true,
        RedirectStandardError = true,
        CreateNoWindow = true
    })!;
    Console.WriteLine(p.StandardOutput.ReadToEnd() + p.StandardError.ReadToEnd());
    p.WaitForExit();
}