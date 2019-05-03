server = tcpip('0.0.0.0', 3000, 'NetWorkRole', 'server');
fopen(server);

i = 1;
even = 1;
odd = 1;
% while 1
%     if server.BytesAvailable > 0
%         data = fread(server, 1, 'int16');
%         if (mod(i - 1, 4) == 0 | mod(i - 1, 4) == 1)
%             signal_even(even) = data;
%             even = even + 1;
%         else
%             signal_odd(odd) = data;
%             odd = odd + 1;
%         end
%         i = i + 1;
%     end
%     if (i > 22050)
%         break
%     end
% end
while 1
    if server.BytesAvailable > 0
        data_3 = fread(server, 1, 'int16');
        if (mod(i - 1, 4) == 0 | mod(i - 1, 4) == 2)
            signal_even_3(even) = data_3;
            even = even + 1;
        else
            signal_odd_3(odd) = data_3;
            odd = odd + 1;
        end
        i = i + 1;
%         i = i + server.bytesAvailable / 2;
%         data_in = fread(server, server.bytesAvailable, 'uint8');
%         size_in = size(data_in)
%         shorts_in = []
%         for j = 1:2:size(data_in)
%             shorts_in = [shorts_in; int16(data_in(j)) + bitshift(int16(data_in(j + 1)), 8)];
%         end
%         data_3 = [data_3; shorts_in];
    end
    if (i > 1794)
        break
    end
end

% subplot(3, 1, 1)
% plot(signal_even_3)
% subplot(3, 1, 2)
% plot(signal_odd_3)

%data_3 = double(data_3);
% signal_odd_3 = data_3(1:2:end);
% signal_even_3 = data_3(2:2:end);

even_size = size(signal_even_3);
odd_size = size(signal_odd_3);
if even_size(2) > odd_size(2)
    min_size = odd_size(2);
else
    min_size = even_size(2);
end

diff_3 = signal_even_3(:,1:min_size) - signal_odd_3(:,1:min_size);
subplot(3, 1, 3)
plot(diff_3)
SPEED_SOUND = 343 ;% m/s
time_odd = generate_time_series(signal_odd_3,44100);
filt_odd = filtered_signal(signal_odd_3,17000,20000);
odd_corr = xcorr(filt_odd, signal);
corr_time_odd = generate_time_series(odd_corr,44100);

time_even = generate_time_series(signal_even_3,44100);
filt_even = filtered_signal(signal_even_3,17000,20000);
ev_corr = xcorr(filt_even, signal);
corr_time_even = generate_time_series(ev_corr,44100);

y = abs(hilbert(ev_corr));

[pks, locs] = findpeaks(y);
[pks, idx] = sort(pks,'descend');
locs = locs(idx);
first_peak = locs(1);
second_peak = 0;
for i = 2:length(locs)
    if locs(i) - locs(1) >= 110
        second_peak = locs(i);
        break
    end
end

dt = (second_peak - locs(1))/44100;
distance = SPEED_SOUND*dt/2
fwrite(server, distance, 'double')