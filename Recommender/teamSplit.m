function [blueX, purpleX] = teamSplit(X)
    [m, n] = size(X);

    blueX = [ones(m,1) X];
    purpleX = [ones(m,1) X(:, n/2+1:n) X(:, 1:n/2)]; 
end