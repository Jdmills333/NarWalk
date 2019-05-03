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
distance = SPEED_SOUND*dt/2;
distance