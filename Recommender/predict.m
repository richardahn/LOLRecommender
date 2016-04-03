function y = predict(X, theta)
    [m, n] = size(X);
    
    [blueX, purpleX] = teamSplit(X);

    blueY = sigmoid(blueX * theta);
    purpleY = sigmoid(purpleX * theta);
    
    y = sigmoid_threshold((blueY + (1 - purpleY)) ./ 2);
end