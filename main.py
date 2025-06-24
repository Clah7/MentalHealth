from fastapi import FastAPI
from pydantic import BaseModel
import numpy as np
import pickle
import uvicorn
from anfis_model import ANFIS

# Load model dan scaler dari file pkl
with open("anfis_model.pkl", "rb") as f:
    data = pickle.load(f)
    model = data["model"]
    scaler = data["scaler"]

# Inisialisasi FastAPI
app = FastAPI(title="ANFIS Stress Level Predictor")

# Input schema
class InputData(BaseModel):
    Gender: str
    Age: int
    Sleep_Duration: float
    Quality_of_Sleep: int
    BMI_Category: str
    Heart_Rate: int
    Daily_Steps: int
    Sleep_Disorder: str

# Helper: Preprocess single input
def preprocess_input(data: InputData):
    gender = 0 if data.Gender.lower() == "male" else 1
    bmi_map = {"normal": 0, "overweight": 1, "obese": 2}
    disorder_map = {"nothing": 0, "sleep apnea": 1, "insomnia": 2}

    bmi = bmi_map.get(data.BMI_Category.lower(), 0)
    disorder = disorder_map.get(data.Sleep_Disorder.lower(), 0)

    input_array = np.array([[gender, data.Age, data.Sleep_Duration, data.Quality_of_Sleep,
                              bmi, data.Heart_Rate, data.Daily_Steps, disorder]])
    
    scaled = scaler.transform(input_array)
    return scaled

# Prediction endpoint
@app.post("/predict", tags=["Prediction"])
def predict_stress(data: InputData):
    try:
        processed = preprocess_input(data)
        prediction = model.predict(processed)
        rounded = int(np.clip(np.round(prediction[0]), 1, 10))  # Skala 1-10
        return {"predicted_stress_level": rounded}
    except Exception as e:
        return {"error": str(e)}

# Jalankan pakai: uvicorn main:app --reload
