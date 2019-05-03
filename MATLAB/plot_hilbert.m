function plot_hilbert(signal, time)
y = hilbert(signal);
plot_param = {'Color', [0.6 0.1 0.2],'Linewidth',2}; 
plot(time,abs(y),plot_param{:})
end