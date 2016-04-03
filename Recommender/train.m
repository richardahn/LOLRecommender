
function fittedTheta = train(X, y, lambda)

% Get the dimension
[m, n] = size(X);

% Append ones to X and create theta with size n+1
X = [ones(m,1) X];
initTheta = zeros(n+1, 1);

options = optimset('GradObj', 'on', 'MaxIter', 5000);
[fittedTheta, ~, ~] = fminunc(@(t) costFunction(t, X, y, lambda), ...
    initTheta, options);


end


