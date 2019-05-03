server = tcpip('0.0.0.0', 3000, 'NetWorkRole', 'server')
fopen(server)

i = 1
while 1
    if server.BytesAvailable > 0
        data = fread(server, 1, 'double');
        signal(i) = data;
    end
    i = i + 1;
    if (i > 441)
        break
    end
end

fwrite(server, data, 'double')