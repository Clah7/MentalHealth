import joblib
import pandas as pd
import numpy as np
import warnings
import skfuzzy as fuzz
from skfuzzy import control as ctrl
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score, confusion_matrix, classification_report
from sklearn.pipeline import Pipeline
from sklearn.base import BaseEstimator, TransformerMixin
import matplotlib.pyplot as plt
import seaborn as sns

warnings.filterwarnings('ignore')

# ----------------------------------------------------------------------------------
# FUNGSI PEMBUAT FITUR FUZZY (Tidak berubah)
# ----------------------------------------------------------------------------------
def create_fuzzy_features(df_input):
    """
    Fungsi ini menghasilkan fitur-fitur fuzzy dari kolom-kolom numerik.
    Logika di dalamnya sama persis dengan kode Anda sebelumnya.
    """
    # Definisi Antecedent (Variabel Input Fuzzy)
    age = ctrl.Antecedent(np.arange(25, 61, 1), 'Age')
    age['young'] = fuzz.trimf(age.universe, [25, 33, 40])
    age['middle'] = fuzz.trimf(age.universe, [38, 44, 59])

    sleep_duration = ctrl.Antecedent(np.arange(5.5, 9.1, 0.1), 'Sleep Duration')
    sleep_duration['short'] = fuzz.trimf(sleep_duration.universe, [5.5, 6.2, 7.0])
    sleep_duration['adequate'] = fuzz.trimf(sleep_duration.universe, [6.8, 8.0, 9.0])
    sleep_duration['long'] = fuzz.trimf(sleep_duration.universe, [8.8, 9.0, 9.0])

    quality_of_sleep = ctrl.Antecedent(np.arange(4, 11, 1), 'Quality of Sleep')
    quality_of_sleep['poor'] = fuzz.trimf(quality_of_sleep.universe, [4, 4, 6])
    quality_of_sleep['average'] = fuzz.trimf(quality_of_sleep.universe, [5, 6.5, 8])
    quality_of_sleep['excellent'] = fuzz.trimf(quality_of_sleep.universe, [7, 9, 10])

    heart_rate = ctrl.Antecedent(np.arange(65, 87, 1), 'Heart Rate')
    heart_rate['normal_low'] = fuzz.trimf(heart_rate.universe, [65, 68, 72])
    heart_rate['normal_mid'] = fuzz.trimf(heart_rate.universe, [70, 75, 80])
    heart_rate['normal_high'] = fuzz.trimf(heart_rate.universe, [78, 82, 86])

    daily_steps = ctrl.Antecedent(np.arange(3000, 10001, 100), 'Daily Steps')
    daily_steps['low'] = fuzz.trimf(daily_steps.universe, [3000, 4500, 6000])
    daily_steps['moderate'] = fuzz.trimf(daily_steps.universe, [5500, 7800, 9000])
    daily_steps['high'] = fuzz.trimf(daily_steps.universe, [8500, 9500, 10000])

    # Proses Fuzzifikasi
    fuzzy_features_list = []
    for index, row in df_input.iterrows():
        fuzzy_features_list.append({
            'Age_young_M': fuzz.interp_membership(age.universe, age['young'].mf, row['Age']),
            'Age_middle_M': fuzz.interp_membership(age.universe, age['middle'].mf, row['Age']),
            'SD_short_M': fuzz.interp_membership(sleep_duration.universe, sleep_duration['short'].mf, row['Sleep Duration']),
            'SD_adequate_M': fuzz.interp_membership(sleep_duration.universe, sleep_duration['adequate'].mf, row['Sleep Duration']),
            'SD_long_M': fuzz.interp_membership(sleep_duration.universe, sleep_duration['long'].mf, row['Sleep Duration']),
            'QoS_poor_M': fuzz.interp_membership(quality_of_sleep.universe, quality_of_sleep['poor'].mf, row['Quality of Sleep']),
            'QoS_average_M': fuzz.interp_membership(quality_of_sleep.universe, quality_of_sleep['average'].mf, row['Quality of Sleep']),
            'QoS_excellent_M': fuzz.interp_membership(quality_of_sleep.universe, quality_of_sleep['excellent'].mf, row['Quality of Sleep']),
            'HR_normal_low_M': fuzz.interp_membership(heart_rate.universe, heart_rate['normal_low'].mf, row['Heart Rate']),
            'HR_normal_mid_M': fuzz.interp_membership(heart_rate.universe, heart_rate['normal_mid'].mf, row['Heart Rate']),
            'HR_normal_high_M': fuzz.interp_membership(heart_rate.universe, heart_rate['normal_high'].mf, row['Heart Rate']),
            'DS_low_M': fuzz.interp_membership(daily_steps.universe, daily_steps['low'].mf, row['Daily Steps']),
            'DS_moderate_M': fuzz.interp_membership(daily_steps.universe, daily_steps['moderate'].mf, row['Daily Steps']),
            'DS_high_M': fuzz.interp_membership(daily_steps.universe, daily_steps['high'].mf, row['Daily Steps'])
        })
    return pd.DataFrame(fuzzy_features_list)


# ----------------------------------------------------------------------------------
# LANGKAH 1: BUAT TRANSFORMER KUSTOM UNTUK PIPELINE
# ----------------------------------------------------------------------------------
class FuzzyHybridTransformer(BaseEstimator, TransformerMixin):
    """
    Transformer kustom Scikit-learn yang melakukan semua langkah preprocessing:
    1. Membersihkan data input.
    2. Melakukan Label Encoding pada kolom kategorikal.
    3. Menghasilkan fitur-fitur fuzzy dari kolom numerik.
    4. Menggabungkan semua fitur menjadi satu set data akhir.
    """
    def __init__(self, categorical_cols=None):
        self.categorical_cols = categorical_cols
        self.encoders_ = {}

    def fit(self, X, y=None):
        """Mempelajari encoder untuk setiap kolom kategorikal."""
        X_copy = X.copy()
        # Ganti nilai dan isi NaN sebelum fitting encoder
        X_copy['BMI Category'] = X_copy['BMI Category'].replace("Normal Weight", "Normal")
        X_copy['Sleep Disorder'] = X_copy['Sleep Disorder'].fillna("Nothing")

        if self.categorical_cols:
            for col in self.categorical_cols:
                le = LabelEncoder()
                self.encoders_[col] = le.fit(X_copy[col])
        return self

    def transform(self, X, y=None):
        """Menerapkan semua transformasi ke data."""
        X_processed = X.copy()

        # 1. Cleaning
        X_processed['BMI Category'] = X_processed['BMI Category'].replace("Normal Weight", "Normal")
        X_processed['Sleep Disorder'] = X_processed['Sleep Disorder'].fillna("Nothing")
        
        # Impute NaNs for numeric cols if any
        numeric_cols = X_processed.select_dtypes(include=np.number).columns
        for col in numeric_cols:
            if X_processed[col].isnull().any():
                 median_val = X_processed[col].median()
                 X_processed[col] = X_processed[col].fillna(median_val)

        # 2. Label Encoding
        if self.categorical_cols:
            for col in self.categorical_cols:
                X_processed[col] = self.encoders_[col].transform(X_processed[col])

        # 3. Fuzzy Feature Generation
        fuzzy_df = create_fuzzy_features(X_processed)

        # 4. Combine features
        X_hybrid = pd.concat([X_processed.reset_index(drop=True), fuzzy_df.reset_index(drop=True)], axis=1)
        
        return X_hybrid


# ----------------------------------------------------------------------------------
# LANGKAH 2: LATIH DAN SIMPAN MODEL MENGGUNAKAN PIPELINE
# ----------------------------------------------------------------------------------
def train_and_save_pipeline():
    """Fungsi utama untuk melatih dan menyimpan pipeline."""
    # Muat data
    try:
        df = pd.read_csv("dataset.csv")
    except FileNotFoundError:
        print("Error: File 'dataset.csv' tidak ditemukan.")
        return

    # Tentukan fitur (X) dan target (y)
    features_to_use = ['Gender', 'Age', 'Sleep Duration', 'Quality of Sleep',
                       'BMI Category', 'Heart Rate', 'Daily Steps', 'Sleep Disorder']
    categorical_features = ['Gender', 'BMI Category', 'Sleep Disorder']
    X = df[features_to_use]
    y = df['Stress Level']

    # Encode target variable (y) secara terpisah
    le_stress = LabelEncoder()
    y_encoded = le_stress.fit_transform(y)

    # Bagi data menjadi set training dan testing
    X_train, X_test, y_train, y_test = train_test_split(X, y_encoded, test_size=0.2, random_state=42)

    # Definisikan pipeline
    # Terdiri dari 2 langkah: preprocessor kustom kita dan classifier
    stress_pipeline = Pipeline(steps=[
        ('preprocessor', FuzzyHybridTransformer(categorical_cols=categorical_features)),
        ('classifier', RandomForestClassifier(random_state=42))
    ])

    # Latih seluruh pipeline
    print("--- Memulai Pelatihan Pipeline ---")
    stress_pipeline.fit(X_train, y_train)
    print("--- Pelatihan Selesai ---")

    # Evaluasi pipeline
    y_pred = stress_pipeline.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    print(f'\nAkurasi Pipeline pada Test Set: {accuracy:.4f}')
    
    print("\nLaporan Klasifikasi Rinci:")
    # PERBAIKAN: Ubah kelas target menjadi string sebelum menampilkannya
    target_names = le_stress.classes_.astype(str)
    print(classification_report(y_test, y_pred, target_names=target_names))

    # Tampilkan Confusion Matrix
    cm = confusion_matrix(y_test, y_pred)
    plt.figure(figsize=(10, 8))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
                xticklabels=target_names, yticklabels=target_names)
    plt.title('Confusion Matrix (Pipeline Model)')
    plt.xlabel('Predicted Label')
    plt.ylabel('True Label')
    plt.show()

    # Simpan pipeline DAN target encoder ke dalam satu file
    # Ini adalah praktik terbaik agar semua yang dibutuhkan untuk prediksi ada di satu tempat
    pipeline_artefact = {
        'pipeline': stress_pipeline,
        'target_encoder': le_stress
    }
    joblib.dump(pipeline_artefact, 'model/stress_pipeline.pkl')
    print("\nPipeline dan target encoder berhasil disimpan ke 'model/stress_pipeline.pkl'")

if __name__ == "__main__":
    train_and_save_pipeline()
