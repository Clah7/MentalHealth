from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import joblib
import pandas as pd
import numpy as np
import skfuzzy as fuzz
from skfuzzy import control as ctrl

# --- LANGKAH 1: Impor Fungsi dari Skrip Training ---
# Pastikan skrip training Anda disimpan sebagai 'model_fuzzy.py' di direktori yang sama.
def create_fuzzy_features(df_input):
    """
    Fungsi ini telah diperbarui sepenuhnya berdasarkan analisis histogram data Anda.
    Baik definisi, rentang (universe), nama kategori, maupun proses fuzzifikasi telah disesuaikan.
    """
    # ------------------ Definisi Antecedent Disesuaikan Berdasarkan Histogram Data Anda ------------------

    # 1. Usia (Age) - Disesuaikan dengan rentang data, kategori 'old' tidak relevan
    age = ctrl.Antecedent(np.arange(25, 61, 1), 'Age')
    age['young'] = fuzz.trimf(age.universe, [25, 33, 40])
    age['middle'] = fuzz.trimf(age.universe, [38, 44, 59])

    # 2. Durasi Tidur (Sleep Duration) - Konfirmasi, definisi awal sudah baik
    sleep_duration = ctrl.Antecedent(np.arange(5.5, 9.1, 0.1), 'Sleep Duration')
    sleep_duration['short'] = fuzz.trimf(sleep_duration.universe, [5.5, 6.2, 7.0])
    sleep_duration['adequate'] = fuzz.trimf(sleep_duration.universe, [6.8, 8.0, 9.0])
    # Kategori 'long' bisa dipertahankan untuk kasus edge, meskipun jarang di data Anda
    sleep_duration['long'] = fuzz.trimf(sleep_duration.universe, [8.8, 9.0, 9.0])

    # 3. Kualitas Tidur (Quality of Sleep) - Disesuaikan minor
    quality_of_sleep = ctrl.Antecedent(np.arange(4, 11, 1), 'Quality of Sleep')
    quality_of_sleep['poor'] = fuzz.trimf(quality_of_sleep.universe, [4, 4, 6])
    quality_of_sleep['average'] = fuzz.trimf(quality_of_sleep.universe, [5, 6.5, 8])
    quality_of_sleep['excellent'] = fuzz.trimf(quality_of_sleep.universe, [7, 9, 10])

    # 4. Denyut Jantung (Heart Rate) - Disesuaikan signifikan ("Zoom In" pada rentang normal)
    heart_rate = ctrl.Antecedent(np.arange(65, 87, 1), 'Heart Rate')
    heart_rate['normal_low'] = fuzz.trimf(heart_rate.universe, [65, 68, 72])
    heart_rate['normal_mid'] = fuzz.trimf(heart_rate.universe, [70, 75, 80])
    heart_rate['normal_high'] = fuzz.trimf(heart_rate.universe, [78, 82, 86])

    # 5. Jumlah Langkah Harian (Daily Steps) - Disesuaikan dengan klaster data
    daily_steps = ctrl.Antecedent(np.arange(3000, 10001, 100), 'Daily Steps')
    daily_steps['low'] = fuzz.trimf(daily_steps.universe, [3000, 4500, 6000])
    daily_steps['moderate'] = fuzz.trimf(daily_steps.universe, [5500, 7800, 9000])
    daily_steps['high'] = fuzz.trimf(daily_steps.universe, [8500, 9500, 10000])

    # ------------------ Proses Fuzzifikasi dengan Nama Kategori Baru ------------------
    fuzzy_features_list = []
    for index, row in df_input.iterrows():
        # Input nilai dari baris data
        current_age = row['Age']
        current_sleep_duration = row['Sleep Duration']
        current_quality_of_sleep = row['Quality of Sleep']
        current_heart_rate = row['Heart Rate']
        current_daily_steps = row['Daily Steps']

        # Fuzzifikasi Usia (Age) - Kategori 'old' dihilangkan
        age_young_m = fuzz.interp_membership(age.universe, age['young'].mf, current_age)
        age_middle_m = fuzz.interp_membership(age.universe, age['middle'].mf, current_age)
        
        # Fuzzifikasi Durasi Tidur (Sleep Duration)
        sd_short_m = fuzz.interp_membership(sleep_duration.universe, sleep_duration['short'].mf, current_sleep_duration)
        sd_adequate_m = fuzz.interp_membership(sleep_duration.universe, sleep_duration['adequate'].mf, current_sleep_duration)
        sd_long_m = fuzz.interp_membership(sleep_duration.universe, sleep_duration['long'].mf, current_sleep_duration)

        # Fuzzifikasi Kualitas Tidur (Quality of Sleep)
        qos_poor_m = fuzz.interp_membership(quality_of_sleep.universe, quality_of_sleep['poor'].mf, current_quality_of_sleep)
        qos_average_m = fuzz.interp_membership(quality_of_sleep.universe, quality_of_sleep['average'].mf, current_quality_of_sleep)
        qos_excellent_m = fuzz.interp_membership(quality_of_sleep.universe, quality_of_sleep['excellent'].mf, current_quality_of_sleep)
        
        # Fuzzifikasi Denyut Jantung (Heart Rate) - Menggunakan nama kategori baru
        hr_normal_low_m = fuzz.interp_membership(heart_rate.universe, heart_rate['normal_low'].mf, current_heart_rate)
        hr_normal_mid_m = fuzz.interp_membership(heart_rate.universe, heart_rate['normal_mid'].mf, current_heart_rate)
        hr_normal_high_m = fuzz.interp_membership(heart_rate.universe, heart_rate['normal_high'].mf, current_heart_rate)

        # Fuzzifikasi Jumlah Langkah (Daily Steps)
        ds_low_m = fuzz.interp_membership(daily_steps.universe, daily_steps['low'].mf, current_daily_steps)
        ds_moderate_m = fuzz.interp_membership(daily_steps.universe, daily_steps['moderate'].mf, current_daily_steps)
        ds_high_m = fuzz.interp_membership(daily_steps.universe, daily_steps['high'].mf, current_daily_steps)

        # Membuat dictionary dengan key yang telah diperbarui
        fuzzy_features_list.append({
            # Age: 'Age_old_M' dihilangkan
            'Age_young_M': age_young_m, 'Age_middle_M': age_middle_m,
            # Sleep Duration: Tetap
            'SD_short_M': sd_short_m, 'SD_adequate_M': sd_adequate_m, 'SD_long_M': sd_long_m,
            # Quality of Sleep: Tetap
            'QoS_poor_M': qos_poor_m, 'QoS_average_M': qos_average_m, 'QoS_excellent_M': qos_excellent_m,
            # Heart Rate: Key disesuaikan dengan nama kategori baru
            'HR_normal_low_M': hr_normal_low_m, 'HR_normal_mid_M': hr_normal_mid_m, 'HR_normal_high_M': hr_normal_high_m,
            # Daily Steps: Tetap
            'DS_low_M': ds_low_m, 'DS_moderate_M': ds_moderate_m, 'DS_high_M': ds_high_m
        })
        
    return pd.DataFrame(fuzzy_features_list)

app = FastAPI(
    title="Stress Level Prediction API",
    description="API to predict stress levels using a Fuzzy-RandomForest hybrid model.",
    version="1.0.0"
)

# Load model dan encoders yang sudah dilatih
try:
    model = joblib.load('stress_model.pkl')
    encoders = joblib.load('encoders.pkl')
    # Ambil nama fitur yang digunakan saat training untuk memastikan konsistensi
    model_feature_names = model.feature_names_in_
except FileNotFoundError:
    raise RuntimeError("Model 'stress_model.pkl' or 'encoders.pkl' not found. Please train the model first.")


# Definisikan struktur data input
class InputData(BaseModel):
    Gender: str
    Age: int
    Sleep_Duration: float
    Quality_of_Sleep: int
    BMI_Category: str
    Heart_Rate: int
    Daily_Steps: int
    Sleep_Disorder: str

    class Config:
        schema_extra = {
            "example": {
                "Gender": "Male",
                "Age": 33,
                "Sleep_Duration": 7.5,
                "Quality_of_Sleep": 8,
                "BMI_Category": "Normal",
                "Heart_Rate": 75,
                "Daily_Steps": 8000,
                "Sleep_Disorder": "Nothing"
            }
        }

@app.get("/", tags=["General"])
def read_root():
    """Provides a simple welcome message."""
    return {"message": "Welcome to the Stress Level Prediction API. Go to /docs for more info."}


@app.post("/predict", tags=["Prediction"])
def predict(data: InputData):
    """
    Predicts the stress level based on input features.
    This endpoint replicates the full feature engineering pipeline from the training script.
    """
    try:
        # --- LANGKAH 2: Ubah Input menjadi DataFrame Pandas ---
        # Nama kolom harus sama persis dengan yang ada di dataset training
        input_dict = {
            'Gender': [data.Gender],
            'Age': [data.Age],
            'Sleep Duration': [data.Sleep_Duration],
            'Quality of Sleep': [data.Quality_of_Sleep],
            'BMI Category': [data.BMI_Category],
            'Heart Rate': [data.Heart_Rate],
            'Daily Steps': [data.Daily_Steps],
            'Sleep Disorder': [data.Sleep_Disorder]
        }
        df_input = pd.DataFrame.from_dict(input_dict)

        # Pre-processing kecil yang sama seperti di training
        df_input['BMI Category'] = df_input['BMI Category'].replace("Normal Weight", "Normal")
        df_input['Sleep Disorder'] = df_input['Sleep Disorder'].fillna("Nothing")

        # --- LANGKAH 3: Buat Fitur Fuzzy ---
        # Panggil fungsi yang sama dari skrip training Anda
        fuzzy_df = create_fuzzy_features(df_input)

        # --- LANGKAH 4: Gabungkan Fitur Asli dan Fuzzy ---
        df_hybrid = pd.concat([df_input.reset_index(drop=True), fuzzy_df.reset_index(drop=True)], axis=1)

        # --- LANGKAH 5: Lakukan Encoding pada Kolom Kategorikal ---
        for col in ['Gender', 'BMI Category', 'Sleep Disorder']:
            if col in encoders:
                # Gunakan transform, bukan fit_transform, karena kita memakai encoder yang sudah ada
                df_hybrid[col] = encoders[col].transform(df_hybrid[col])
            else:
                raise ValueError(f"Encoder for column '{col}' not found.")
        
        # --- LANGKAH 6: Pastikan Urutan Kolom Benar ---
        # Ini adalah langkah pengamanan untuk memastikan DataFrame memiliki
        # semua 22 kolom dalam urutan yang sama persis seperti saat model dilatih.
        final_input_df = df_hybrid.reindex(columns=model_feature_names, fill_value=0)

        # --- Lakukan Prediksi ---
        prediction_encoded = model.predict(final_input_df)
        
        # Decode hasil prediksi ke label yang bisa dibaca
        stress_level_numpy = encoders['Stress Level'].inverse_transform(prediction_encoded)[0]
        
        # --- PERBAIKAN: Konversi tipe data NumPy ke tipe data standar Python ---
        stress_level = str(stress_level_numpy)
        
        return {"predicted_stress_level": stress_level}
    
    except KeyError as e:
        raise HTTPException(status_code=400, detail=f"Invalid category value provided. Error: {e}")
    except Exception as e:
        # Tangkap error lain untuk debugging
        raise HTTPException(status_code=500, detail=f"An unexpected error occurred: {str(e)}")

