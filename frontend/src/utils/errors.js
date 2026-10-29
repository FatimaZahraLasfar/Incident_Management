/** Message lisible à partir d'une erreur Axios ({status, message} renvoyé par le backend). */
export function getErrorMessage(error, fallback = 'Une erreur est survenue. Réessayez.') {
  if (error?.response?.data?.message) {
    return error.response.data.message;
  }
  if (error?.request && !error.response) {
    return 'Le serveur est injoignable. Vérifiez que le backend est démarré.';
  }
  return fallback;
}

/** Erreurs de validation par champ ({ title: "Le titre est obligatoire" }) ou objet vide. */
export function getFieldErrors(error) {
  return error?.response?.data?.errors || {};
}
