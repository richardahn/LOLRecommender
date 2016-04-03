%% Load data
clear; close all; clc;

data = load('../Data/fetchedmatches.txt');
%% Settings and Details
partSize = 0.91;     % Partition size for training set
n = size(data,2)-1;  % # of features
m = size(data,1);    % # of examples
lambda = 0;

[training, test, mTraining, mTest] = partition(data, partSize);

% Get training input/target
XTraining = training(:,1:n);
yTraining = training(:,n+1);

% Get test input/target
XTest = test(:,1:n);
yTest = test(:j5,n+1);

fittedTheta = train(XTraining, yTraining, lambda);
trainingPredictions = predict(XTraining, fittedTheta);
testPredictions = predict(XTest, fittedTheta);

trainingAccuracy = mean(double(trainingPredictions==yTraining)) * 100
testAccuracy = mean(double(testPredictions==yTest)) * 100