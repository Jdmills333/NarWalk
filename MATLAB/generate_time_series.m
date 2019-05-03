function [timeValues] = generate_time_series(signal,sampleRate)
%NORMALIZ Summary of this function goes here
%   Detailed explanation goes here
startTime = 0;
timeValues = startTime + (0:length(signal)-1).'/sampleRate;
end