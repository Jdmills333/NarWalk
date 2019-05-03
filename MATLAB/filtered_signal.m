function [filt,time] = filtered_signal(signal,startFreq, stopFreq)
%NORMALIZ Summary of this function goes here
%   Detailed explanation goes here
sampleRate = 44100;
time = generate_time_series(signal,sampleRate);
filt = bandPass(signal,time,startFreq,stopFreq);
end