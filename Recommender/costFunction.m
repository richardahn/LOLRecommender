function [J, grad] = costFunction(theta, X, y, lambda)
    
    m = length(y);
    
    regTheta = theta; 
    regTheta(1) = 0;
    
    h = sigmoid(X*theta);
    cost = y'*log(h) + (1-y)'*log(1-sigmoid(h));
    J = -(1/m) * (cost + lambda/2*regTheta'*regTheta);
    grad = (1/m) * (X'*(h-y) + lambda*regTheta);
end
