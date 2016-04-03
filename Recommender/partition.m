function [training, test, mTraining, mTest] = partition(X, frac)
    count = size(X,1);
    mTraining = double(int64(frac*count));
    mTest = double(int64((1-frac)*count));
    inc = 1 + (mTraining / mTest);
    test = X(1:inc:count,:);
    X(1:inc:count,:) = [];
    training = X;
    