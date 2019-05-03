server = tcpip('0.0.0.0', 3000, 'NetWorkRole', 'server');
fopen(server);
numSamples = 220;
i = 1;
signal = zeros(numSamples, 1);
while 1
    if server.BytesAvailable > 0
        signal(i) = fread(server, 1, 'double');
        i = i + 1;
        if i > numSamples
            break
        end
    end
end
