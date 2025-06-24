from anfis_model import ANFIS
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
import pickle


def preprocess_data():
    df = pd.read_csv("dataset.csv")
    features = ['Gender', 'Age', 'Sleep Duration', 'Quality of Sleep',
                'BMI Category', 'Heart Rate', 'Daily Steps', 'Sleep Disorder']
    X = df[features].copy()
    y = df['Stress Level'].copy()
    X['BMI Category'] = X['BMI Category'].replace("Normal Weight", "Normal")
    X['Sleep Disorder'] = X['Sleep Disorder'].fillna("Nothing")
    X['Sleep Disorder'] = X['Sleep Disorder'].replace("None", "Nothing")
    X['Gender'] = X['Gender'].map({'Male': 0, 'Female': 1})
    X['BMI Category'] = X['BMI Category'].map(
        {'Normal': 0, 'Overweight': 1, 'Obese': 2})
    X['Sleep Disorder'] = X['Sleep Disorder'].map(
        {'Nothing': 0, 'Sleep Apnea': 1, 'Insomnia': 2})
    return X, y


if __name__ == "__main__":
    print("Loading and preprocessing data...")
    X, y = preprocess_data()
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    anfis = ANFIS(
        n_inputs=X_train_scaled.shape[1],
        n_rules=3,
        learning_rate=0.01,
        epochs=100
    )
    anfis.fit(X_train_scaled, y_train.values)
    with open("anfis_model.pkl", "wb") as f:
        pickle.dump({"model": anfis, "scaler": scaler}, f)
    print("Model and scaler saved as anfis_model.pkl")
